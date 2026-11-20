package com.example.motormates.data.source

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Nombres de modelo y límites de AI Tuning. Viven en Remote Config porque
 * los nombres de los modelos de Gemini cambian cada pocos meses: así se
 * actualizan desde la consola sin publicar otra versión de la app.
 */
interface AiModelConfig {
    suspend fun imageModel(): String
    suspend fun textModel(): String

    /** Se usa cuando el modelo de texto principal está saturado (503). */
    suspend fun textFallbackModel(): String
    /** Imágenes por usuario al día; null = sin límite (mientras se desarrolla). */
    suspend fun dailyGenerationLimit(): Int?

    /** true: Vertex AI Gemini API; false: Gemini Developer API. */
    suspend fun useVertexAi(): Boolean
}

@Singleton
class RemoteConfigAiModelConfig @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig
) : AiModelConfig {

    private val mutex = Mutex()
    private var fetched = false

    override suspend fun imageModel(): String =
        ensureFetched().getString(KEY_IMAGE_MODEL).ifBlank { DEFAULT_IMAGE_MODEL }

    override suspend fun textModel(): String =
        ensureFetched().getString(KEY_TEXT_MODEL).ifBlank { DEFAULT_TEXT_MODEL }

    override suspend fun textFallbackModel(): String =
        ensureFetched().getString(KEY_TEXT_FALLBACK_MODEL).ifBlank { DEFAULT_TEXT_FALLBACK_MODEL }

    override suspend fun dailyGenerationLimit(): Int? =
        ensureFetched().getLong(KEY_DAILY_LIMIT).toInt().takeIf { it > 0 }

    override suspend fun useVertexAi(): Boolean =
        ensureFetched().getString(KEY_BACKEND).ifBlank { DEFAULT_BACKEND } != BACKEND_GOOGLE_AI

    /**
     * Una sola vez por proceso y con tiempo límite: si no hay red o la
     * consola no tiene los parámetros, se usan los valores por defecto.
     */
    private suspend fun ensureFetched(): FirebaseRemoteConfig = mutex.withLock {
        if (!fetched) {
            runCatching {
                remoteConfig.setConfigSettingsAsync(
                    remoteConfigSettings { minimumFetchIntervalInSeconds = FETCH_INTERVAL_SECONDS }
                ).await()
                remoteConfig.setDefaultsAsync(DEFAULTS).await()
                withTimeoutOrNull(FETCH_TIMEOUT_MS) { remoteConfig.fetchAndActivate().await() }
            }
            fetched = true
        }
        remoteConfig
    }

    companion object {
        const val KEY_IMAGE_MODEL = "ai_image_model"
        const val KEY_TEXT_MODEL = "ai_text_model"
        const val KEY_TEXT_FALLBACK_MODEL = "ai_text_fallback_model"
        const val KEY_DAILY_LIMIT = "ai_daily_limit"
        const val KEY_BACKEND = "ai_backend"

        const val BACKEND_VERTEX_AI = "vertex_ai"
        const val BACKEND_GOOGLE_AI = "google_ai"

        /**
         * Modelos estables de la lista oficial de Firebase AI Logic (noviembre
         * de 2026). Se evita gemini-nano-banana-2.1: está marcado como de
         * "disponibilidad a corto plazo" y Google lo retirará pronto.
         */
        const val DEFAULT_IMAGE_MODEL = "gemini-3.1-flash-image"
        const val DEFAULT_TEXT_MODEL = "gemini-3.8-flash"
        const val DEFAULT_TEXT_FALLBACK_MODEL = "gemini-3.5-flash-lite"
        /** 0 = sin límite. Para volver a limitar, poner ai_daily_limit en Remote Config. */
        const val DEFAULT_DAILY_LIMIT = 0
        const val DEFAULT_BACKEND = BACKEND_VERTEX_AI

        private const val FETCH_INTERVAL_SECONDS = 3600L
        private const val FETCH_TIMEOUT_MS = 4_000L

        private val DEFAULTS: Map<String, Any> = mapOf(
            KEY_IMAGE_MODEL to DEFAULT_IMAGE_MODEL,
            KEY_TEXT_MODEL to DEFAULT_TEXT_MODEL,
            KEY_TEXT_FALLBACK_MODEL to DEFAULT_TEXT_FALLBACK_MODEL,
            KEY_DAILY_LIMIT to DEFAULT_DAILY_LIMIT.toLong(),
            KEY_BACKEND to DEFAULT_BACKEND
        )
    }
}
