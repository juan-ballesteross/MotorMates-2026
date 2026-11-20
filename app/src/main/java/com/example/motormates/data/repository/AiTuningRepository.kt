package com.example.motormates.data.repository

import android.net.Uri
import android.util.Log
import com.example.motormates.data.dto.InsightsDto
import com.example.motormates.data.model.GroundedAnswer
import com.example.motormates.data.model.Part
import com.example.motormates.data.model.TuningImage
import com.example.motormates.data.model.TuningInsights
import com.example.motormates.data.model.TuningMode
import com.example.motormates.data.model.TuningSummary
import com.example.motormates.data.model.Vehicle
import com.example.motormates.data.source.AiModelConfig
import com.example.motormates.data.source.AiUsageDataSource
import com.example.motormates.data.source.GeminiTuningDataSource
import com.example.motormates.data.source.ImageEditSession
import com.example.motormates.data.source.NoImageReturnedException
import com.example.motormates.data.source.TuningImageSource
import com.example.motormates.data.util.TimeSource
import com.example.motormates.data.util.TuningPromptBuilder
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.ai.type.APINotConfiguredException
import com.google.firebase.ai.type.FirebaseAIException
import com.google.firebase.ai.type.InvalidAPIKeyException
import com.google.firebase.ai.type.PromptBlockedException
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.RequestTimeoutException
import com.google.firebase.ai.type.ResponseStoppedException
import com.google.firebase.ai.type.ServerException
import com.google.firebase.ai.type.ServiceDisabledException
import com.google.gson.Gson
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/** Qué se le pide a la AI que dibuje. */
data class TuningRequest(
    val vehicle: Vehicle,
    val mode: TuningMode,
    val parts: List<Part> = emptyList(),
    val ownPartImage: TuningImage? = null,
    val ownPartDescription: String = "",
    val freeIdea: String = ""
)

data class GeneratedImage(
    val image: TuningImage,
    val prompt: String,
    /** Generaciones que le quedan hoy al usuario. */
    /** Imágenes que le quedan hoy; null = sin límite. */
    val remainingToday: Int?
)

/** Error con mensaje listo para mostrar. */
class TuningException(message: String, cause: Throwable? = null) : Exception(message, cause)

interface AiTuningRepository {
    /** Genera la imagen modificada y abre una sesión para pedir ajustes con [refine]. */
    suspend fun generate(request: TuningRequest): Result<GeneratedImage>

    /** Ajuste conversacional sobre la última imagen ("más bajo", "otro color"). */
    suspend fun refine(instruction: String): Result<GeneratedImage>

    /** Ficha técnica: la AI explica los números que ya calculó la app. */
    suspend fun explain(vehicle: Vehicle, parts: List<Part>, summary: TuningSummary): Result<TuningInsights>

    /** Precio actual de una pieza con búsqueda de Google (con fuentes). */
    suspend fun livePrice(part: Part): Result<GroundedAnswer>

    /** Identifica una pieza fotografiada por el usuario y busca su precio. */
    suspend fun identifyPart(image: TuningImage, description: String): Result<GroundedAnswer>

    /** Carga la foto elegida por el usuario y le quita el fondo. */
    suspend fun prepareOwnPart(uri: Uri): Result<TuningImage>

    suspend fun remainingGenerations(): Int?
}

/**
 * Singleton con estado: guarda la sesión de edición abierta para que
 * [refine] continúe la misma conversación. Solo hay una pantalla de tuning a
 * la vez, así que basta con una sesión; [generate] la reemplaza.
 */
@Singleton
class AiTuningRepositoryImpl @Inject constructor(
    private val gemini: GeminiTuningDataSource,
    private val images: TuningImageSource,
    private val usage: AiUsageDataSource,
    private val config: AiModelConfig,
    private val authRepository: AuthRepository,
    private val timeSource: TimeSource
) : AiTuningRepository {

    private val gson = Gson()
    private var session: ImageEditSession? = null

    override suspend fun generate(request: TuningRequest): Result<GeneratedImage> = runTuning {
        val baseUrl = request.vehicle.imageUrl
            ?: throw TuningException("Este vehículo no tiene foto para modificar")

        val base = runCatching { images.loadFromUrl(baseUrl) }.getOrElse {
            throw TuningException("No se pudo descargar la foto del vehículo", it)
        }

        val (prompt, references) = when (request.mode) {
            TuningMode.CATALOG -> {
                if (request.parts.isEmpty()) throw TuningException("Elige al menos una pieza")
                // Solo las piezas visibles con foto se mandan como referencia;
                // si una foto falla, esa pieza se describe solo con texto.
                val withImage = request.parts
                    .filter { it.visible && it.imageUrl != null }
                    .mapNotNull { part ->
                        runCatching { part.id to images.loadFromUrl(part.imageUrl!!) }.getOrNull()
                    }
                    .take(MAX_REFERENCE_IMAGES)
                TuningPromptBuilder.catalogImagePrompt(
                    request.vehicle, request.parts, withImage.map { it.first }
                ) to withImage.map { it.second }
            }
            TuningMode.OWN_PART -> {
                val partImage = request.ownPartImage
                    ?: throw TuningException("Primero toma o elige la foto de la pieza")
                TuningPromptBuilder.ownPartImagePrompt(request.vehicle, request.ownPartDescription) to
                    listOf(partImage)
            }
            TuningMode.FREE -> {
                if (request.freeIdea.isBlank()) throw TuningException("Describe tu idea primero")
                TuningPromptBuilder.freeImagePrompt(request.vehicle, request.freeIdea) to emptyList()
            }
        }

        // Se cuenta justo antes de llamar al modelo: si algo falla antes
        // (foto, validaciones) no se gasta una generación del día.
        val remaining = consumeGeneration()
        val newSession = gemini.startImageSession()
        val image = newSession.send(prompt, listOf(base) + references)
        session = newSession
        GeneratedImage(image, prompt, remaining)
    }

    override suspend fun refine(instruction: String): Result<GeneratedImage> = runTuning {
        val current = session ?: throw TuningException("Primero genera una imagen")
        if (instruction.isBlank()) throw TuningException("Escribe qué quieres cambiar")
        val remaining = consumeGeneration()
        val prompt = TuningPromptBuilder.refinePrompt(instruction)
        GeneratedImage(current.send(prompt), prompt, remaining)
    }

    override suspend fun explain(
        vehicle: Vehicle,
        parts: List<Part>,
        summary: TuningSummary
    ): Result<TuningInsights> = runTuning {
        val json = gemini.generateInsightsJson(TuningPromptBuilder.insightsPrompt(vehicle, parts, summary))
        parseInsights(json)
    }

    override suspend fun livePrice(part: Part): Result<GroundedAnswer> = runTuning {
        gemini.searchGrounded(TuningPromptBuilder.livePricePrompt(part))
    }

    override suspend fun identifyPart(image: TuningImage, description: String): Result<GroundedAnswer> =
        runTuning { gemini.searchGrounded(TuningPromptBuilder.identifyPartPrompt(description), image) }

    override suspend fun prepareOwnPart(uri: Uri): Result<TuningImage> = runTuning {
        val original = runCatching { images.loadFromUri(uri) }.getOrElse {
            throw TuningException("No se pudo leer la foto de la pieza", it)
        }
        images.removeBackground(original)
    }

    override suspend fun remainingGenerations(): Int? {
        val uid = authRepository.getCurrentUser()?.id ?: return null
        return runCatching {
            val limit = config.dailyGenerationLimit() ?: return null
            (limit - usage.usedToday(uid, today())).coerceAtLeast(0)
        }.getOrNull()
    }

    internal fun parseInsights(json: String): TuningInsights {
        val dto = runCatching { gson.fromJson(json, InsightsDto::class.java) }.getOrNull()
            ?: throw TuningException("La ficha técnica llegó en un formato inesperado")
        return dto.toDomain()
    }

    /** Cuenta una generación. Sin límite configurado no toca aiUsage y devuelve null. */
    private suspend fun consumeGeneration(): Int? {
        val uid = authRepository.getCurrentUser()?.id
            ?: throw TuningException("Debes iniciar sesión para usar AI Tuning")
        val limit = config.dailyGenerationLimit() ?: return null
        val consumed = usage.tryConsume(uid, today(), limit)
        if (!consumed) {
            throw TuningException("Llegaste al límite de $limit imágenes por hoy. Vuelve mañana.")
        }
        return (limit - usage.usedToday(uid, today())).coerceAtLeast(0)
    }

    private fun today(): String =
        Instant.ofEpochMilli(timeSource.nowMillis()).atZone(ZoneId.systemDefault()).toLocalDate().toString()

    /** Traduce los errores de la AI y de red a mensajes en español. */
    private inline fun <T> runTuning(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: TuningException) {
        Result.failure(e)
    } catch (e: NoImageReturnedException) {
        Result.failure(
            TuningException(
                "La AI no pudo generar la imagen con esa petición. Prueba con otras piezas o redacta distinto.",
                e
            )
        )
    } catch (e: Exception) {
        // El mensaje en español es genérico; el detalle real (p. ej. "App Check
        // token is invalid") queda en logcat con la etiqueta AiTuning.
        // runCatching: en las pruebas JVM android.util.Log no está disponible.
        runCatching { Log.e(LOG_TAG, "Fallo en AI Tuning", e) }
        Result.failure(TuningException(e.toSpanishMessage(), e))
    }

    private fun Exception.toSpanishMessage(): String = when (this) {
        is QuotaExceededException -> "Se agotó la cuota de Gemini del proyecto. Intenta más tarde."
        is PromptBlockedException, is ResponseStoppedException ->
            "La AI rechazó la petición por sus filtros de seguridad. Prueba con otra idea."
        is RequestTimeoutException -> "La AI tardó demasiado en responder. Intenta de nuevo."
        is ServiceDisabledException, is APINotConfiguredException ->
            "Firebase AI Logic no está habilitado en el proyecto (revisa la consola y el plan Blaze)."
        is InvalidAPIKeyException -> "La configuración de Firebase de la app no es válida."
        is FirebaseNetworkException, is IOException -> "Sin conexión a internet, verifica tu red"
        is ServerException ->
            if (message.orEmpty().contains("unavailable", ignoreCase = true)) {
                "Los servidores de Gemini están saturados. Intenta de nuevo en un minuto."
            } else {
                "La AI no está disponible en este momento. Intenta de nuevo."
            }
        is FirebaseAIException -> "La AI no está disponible en este momento. Intenta de nuevo."
        else -> "Algo salió mal con AI Tuning. Intenta de nuevo."
    }

    private companion object {
        /** El modelo acepta hasta 14 imágenes; se dejan pocas para que responda rápido. */
        const val MAX_REFERENCE_IMAGES = 6
        const val LOG_TAG = "AiTuning"
    }
}
