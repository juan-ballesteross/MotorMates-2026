package com.example.motormates.ui.reviewsMap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.Review
import com.example.motormates.data.model.ReviewMapMarkerUi
import com.example.motormates.data.model.toReviewMapMarkerUi
import com.example.motormates.data.repository.ReviewRepository
import com.example.motormates.data.util.toEpochMillisOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * El mapa de reseñas recientes.
 *
 * La ventana de 24 horas se decide acá y no en la capa de datos: es una
 * decisión de producto ("para no saturar el mapa"), así que el repositorio
 * queda genérico y recibe la fecha de corte ya calculada.
 */
@HiltViewModel
class ReviewsMapViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewsMapUiState())
    val uiState: StateFlow<ReviewsMapUiState> = _uiState

    private var markersJob: Job? = null

    /**
     * Abre (o reabre) la escucha de reseñas recientes. Se vuelve a llamar cada
     * vez que se entra a la pestaña del mapa, y eso importa: la fecha de corte
     * se calcula aquí, así que reabrir la consulta es lo que mantiene la
     * ventana de 24 horas fresca en vez de congelada en el momento en que se
     * creó el ViewModel.
     */
    fun start() {
        markersJob?.cancel()
        markersJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            reviewRepository.observeRecentReviews(sinceMillis = recentWindowStart()).collect { result ->
                _uiState.update { state ->
                    result.fold(
                        onSuccess = { reviews ->
                            state.copy(
                                isLoading = false,
                                errorMessage = null,
                                markers = reviews.toMarkers()
                            )
                        },
                        onFailure = { error ->
                            state.copy(
                                isLoading = false,
                                markers = emptyList(),
                                errorMessage = error.message ?: "No se pudo cargar el mapa de reseñas"
                            )
                        }
                    )
                }
            }
        }
    }

    fun onMarkerClick(markerId: String) {
        _uiState.update { it.copy(selectedMarkerId = markerId) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedMarkerId = null) }
    }

    fun retry() = start()

    /**
     * Dos recortes, no uno:
     *
     * - La consulta ya pidió solo lo publicado después de la fecha de corte,
     *   que es lo que evita traerse la colección entera.
     * - Aquí se vuelve a filtrar contra la hora actual porque el listener sigue
     *   vivo mientras la pantalla esté abierta: sin esto, una reseña que cumple
     *   24 horas con el mapa abierto se quedaría pegada en pantalla.
     *
     * La fecha se compara en millis y no como texto ISO: Instant.toString()
     * escribe los segundos fraccionarios con precisión variable, así que el
     * orden alfabético no coincide con el cronológico.
     */
    private fun List<Review>.toMarkers(): List<ReviewMapMarkerUi> {
        val cutoff = recentWindowStart()
        return filter { review ->
            val createdAt = review.createdAt.toEpochMillisOrNull()
            createdAt != null && createdAt >= cutoff
        }
            // mapNotNull descarta las reseñas sin ubicación: no hay dónde
            // ponerles un alfiler.
            .mapNotNull { it.toReviewMapMarkerUi() }
    }

    private fun recentWindowStart(): Long =
        System.currentTimeMillis() - RECENT_WINDOW_MS

    private companion object {
        const val RECENT_WINDOW_HOURS = 24L
        const val RECENT_WINDOW_MS = RECENT_WINDOW_HOURS * 60 * 60 * 1000
    }
}
