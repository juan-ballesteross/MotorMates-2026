package com.example.motormates.data.source

import com.example.motormates.data.model.GroundedAnswer
import com.example.motormates.data.model.GroundingSource
import com.example.motormates.data.model.TuningImage
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.ServerException
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import javax.inject.Inject
import kotlinx.coroutines.delay

/**
 * Único punto de la app que habla con Firebase AI Logic. Todo lo demás
 * trabaja con [TuningImage] (bytes) y modelos de dominio, para poder probar
 * sin red ni Android.
 */
interface GeminiTuningDataSource {

    /** Abre una conversación de edición: permite pedir ajustes sobre la última imagen. */
    suspend fun startImageSession(): ImageEditSession

    /** Ficha técnica en JSON (con responseSchema). Devuelve el JSON crudo. */
    suspend fun generateInsightsJson(prompt: String): String

    /** Respuesta con búsqueda de Google; opcionalmente con una imagen de entrada. */
    suspend fun searchGrounded(prompt: String, image: TuningImage? = null): GroundedAnswer
}

interface ImageEditSession {
    /**
     * Manda texto e imágenes y devuelve la imagen generada.
     * @throws NoImageReturnedException si el modelo respondió sin imagen.
     */
    suspend fun send(prompt: String, images: List<TuningImage> = emptyList()): TuningImage
}

/** El modelo respondió solo texto (por ejemplo, se negó a editar). */
class NoImageReturnedException(val modelText: String?) :
    Exception("El modelo no devolvió una imagen")

class FirebaseAiTuningDataSource @Inject constructor(
    private val config: AiModelConfig
) : GeminiTuningDataSource {

    /**
     * Proveedor según Remote Config (`ai_backend`). Vertex AI se cobra a la
     * cuenta de facturación de Google Cloud (donde están los créditos de la
     * prueba); la Gemini Developer API exige créditos de prepago en AI Studio.
     */
    private suspend fun ai() = Firebase.ai(
        backend = if (config.useVertexAi()) {
            GenerativeBackend.vertexAI(location = VERTEX_LOCATION)
        } else {
            GenerativeBackend.googleAI()
        }
    )

    override suspend fun startImageSession(): ImageEditSession {
        val model = ai().generativeModel(
            modelName = config.imageModel(),
            generationConfig = generationConfig {
                // TEXT + IMAGE: si el modelo no puede editar, explica por qué
                // en texto en vez de fallar en silencio.
                responseModalities = listOf(ResponseModality.TEXT, ResponseModality.IMAGE)
                imageConfig = ImageConfig.builder()
                    .setAspectRatio(AspectRatio.LANDSCAPE_4x3)
                    .build()
            }
        )
        return FirebaseImageEditSession(model.startChat())
    }

    override suspend fun generateInsightsJson(prompt: String): String =
        withTextModelFallback { modelName ->
            val model = ai().generativeModel(
                modelName = modelName,
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = INSIGHTS_SCHEMA
                }
            )
            model.generateContent(prompt).text
                ?: throw IllegalStateException("La ficha técnica llegó vacía")
        }

    override suspend fun searchGrounded(prompt: String, image: TuningImage?): GroundedAnswer =
        withTextModelFallback { modelName ->
            val model = ai().generativeModel(
                modelName = modelName,
                tools = listOf(Tool.googleSearch())
            )
            val response = if (image == null) {
                model.generateContent(prompt)
            } else {
                model.generateContent(
                    content {
                        inlineData(image.bytes, image.mimeType())
                        text(prompt)
                    }
                )
            }
            response.toGroundedAnswer()
        }

    /**
     * Gemini responde 503 ("The service is currently unavailable") cuando el
     * modelo está saturado. Primero se reintenta con el modelo principal con
     * espera creciente; si sigue saturado, se pasa al modelo de respaldo
     * (más liviano, con más capacidad) antes de mostrarle el error al usuario.
     */
    private suspend fun <T> withTextModelFallback(block: suspend (modelName: String) -> T): T {
        val primary = config.textModel()
        return try {
            retryingWhenUnavailable { block(primary) }
        } catch (e: ServerException) {
            val fallback = config.textFallbackModel()
            if (!e.isTransient() || fallback == primary) throw e
            retryingWhenUnavailable { block(fallback) }
        }
    }

    private suspend fun <T> retryingWhenUnavailable(block: suspend () -> T): T {
        repeat(RETRY_DELAYS_MS.size) { attempt ->
            try {
                return block()
            } catch (e: ServerException) {
                if (!e.isTransient()) throw e
                delay(RETRY_DELAYS_MS[attempt])
            }
        }
        return block()
    }

    private fun ServerException.isTransient(): Boolean {
        val text = message.orEmpty().lowercase()
        return "unavailable" in text || "overloaded" in text || "503" in text
    }

    private class FirebaseImageEditSession(private val chat: Chat) : ImageEditSession {
        override suspend fun send(prompt: String, images: List<TuningImage>): TuningImage {
            val response = chat.sendMessage(
                content {
                    images.forEach { inlineData(it.bytes, it.mimeType()) }
                    text(prompt)
                }
            )
            val imagePart = response.inlineDataParts
                .firstOrNull { it.mimeType.startsWith("image/") && !it.isThought }
                ?: throw NoImageReturnedException(response.textOrNull())
            return TuningImage(imagePart.inlineData)
        }
    }

    companion object {

        /** Los modelos de imagen en preview suelen estar solo en "global". */
        private const val VERTEX_LOCATION = "global"

        private val RETRY_DELAYS_MS = longArrayOf(1_500, 4_000)

        /** Debe coincidir con InsightsDto (Gson lo lee con esos nombres). */
        private val INSIGHTS_SCHEMA = Schema.obj(
            mapOf(
                "summary" to Schema.string(),
                "handling" to Schema.string(),
                "consumption" to Schema.string(),
                "warranty" to Schema.string(),
                "legal" to Schema.array(Schema.string()),
                "tips" to Schema.array(Schema.string())
            )
        )
    }
}

private fun GenerateContentResponse.textOrNull(): String? = runCatching { text }.getOrNull()

/**
 * Las fuentes y las sugerencias de búsqueda son obligatorias de mostrar
 * según los términos de uso del grounding con Google Search.
 */
private fun GenerateContentResponse.toGroundedAnswer(): GroundedAnswer {
    val metadata = candidates.firstOrNull()?.groundingMetadata
    val sources = metadata?.groundingChunks.orEmpty()
        .mapNotNull { chunk ->
            val web = chunk.web ?: return@mapNotNull null
            val uri = web.uri ?: return@mapNotNull null
            GroundingSource(title = web.title ?: web.domain ?: uri, uri = uri)
        }
        .distinctBy { it.uri }
    return GroundedAnswer(
        text = textOrNull().orEmpty().trim(),
        sources = sources,
        searchEntryPointHtml = metadata?.searchEntryPoint?.renderedContent
    )
}
