package com.example.motormates.ui.review

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.ReviewAspect
import com.example.motormates.data.model.toReviewCarSummary
import com.example.motormates.data.repository.ReviewRepository
import com.example.motormates.data.repository.VehicleRepository
import com.example.motormates.data.source.DeviceLocationDataSource
import com.example.motormates.data.source.PlaceNameDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class NewReviewViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val reviewRepository: ReviewRepository,
    private val deviceLocationDataSource: DeviceLocationDataSource,
    private val placeNameDataSource: PlaceNameDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewReviewUiState())
    val uiState: StateFlow<NewReviewUiState> = _uiState

    /**
     * Resuelve el vehículo que se está reseñando (antes lo hacía el grafo de
     * navegación a partir de los mocks) y, en modo edición, precarga el
     * formulario. La reseña se busca dentro de las del vehículo — la misma
     * consulta que ya hace la pantalla de detalle. reviewId null = crear.
     */
    fun start(vehicleId: String, reviewId: String?) {
        if (_uiState.value.car != null) return // ya cargado: no repetir en recomposiciones

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val vehicleResult = vehicleRepository.getVehicleById(vehicleId)
            val vehicle = vehicleResult.getOrElse { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "No se pudo cargar el vehículo"
                    )
                }
                return@launch
            }

            val isEditing = reviewId != null
            val existing = if (isEditing) {
                reviewRepository.getReviewsByVehicle(vehicleId)
                    .getOrNull()
                    ?.firstOrNull { it.id == reviewId }
            } else {
                null
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    car = vehicle.toReviewCarSummary(),
                    reviewId = existing?.id,
                    rating = existing?.rating ?: 0,
                    experience = existing?.comment.orEmpty(),
                    existingImageUrls = existing?.imageUrls.orEmpty(),
                    errorMessage = if (isEditing && existing == null) {
                        "No se encontró la reseña que querías editar"
                    } else {
                        null
                    }
                )
            }
        }
    }

    /**
     * Resuelve la ubicación y su nombre para mostrarlos en el formulario.
     * La llama la pantalla al entrar (solo al crear), después de resolver el
     * permiso: así el usuario ve qué se va a adjuntar antes de publicar, y
     * publicar no tiene que esperar al GPS.
     */
    fun captureLocation() {
        if (_uiState.value.isLocationLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLocationLoading = true, isLocationUnavailable = false) }

            val location = deviceLocationDataSource.lastKnownLocation()
            // El nombre es un adorno: si no se resuelve, la ubicación sigue
            // sirviendo para el mapa y solo se muestran las coordenadas sin
            // texto.
            val named = location?.let { it.copy(placeName = placeNameDataSource.placeNameFor(it)) }

            _uiState.update {
                it.copy(
                    isLocationLoading = false,
                    capturedLocation = named,
                    isLocationUnavailable = named == null
                )
            }
        }
    }

    /** El usuario decide no adjuntar la ubicación (la ✕ de la fila). */
    fun removeLocation() {
        _uiState.update {
            it.copy(capturedLocation = null, isLocationUnavailable = false, isLocationLoading = false)
        }
    }

    fun updateRating(rating: Int) {
        _uiState.update { it.copy(rating = rating, errorMessage = null) }
    }

    fun updateExperience(input: String) {
        _uiState.update { it.copy(experience = input) }
    }

    fun toggleAspect(aspect: ReviewAspect) {
        _uiState.update { current ->
            val selectedAspects = if (aspect in current.selectedAspects) {
                current.selectedAspects - aspect
            } else {
                current.selectedAspects + aspect
            }
            current.copy(selectedAspects = selectedAspects)
        }
    }

    fun addPhoto(uri: Uri) {
        _uiState.update { current ->
            val totalPhotos = current.existingImageUrls.size + current.newPhotoUris.size
            if (totalPhotos < 3) current.copy(newPhotoUris = current.newPhotoUris + uri) else current
        }
    }

    fun removeNewPhoto(uri: Uri) {
        _uiState.update { it.copy(newPhotoUris = it.newPhotoUris - uri) }
    }

    fun removeExistingPhoto(url: String) {
        _uiState.update { it.copy(existingImageUrls = it.existingImageUrls - url) }
    }

    /**
     * Valida del lado del cliente antes de llamar al backend. El backend
     * acepta rating 0, pero las estrellas solo expresan 1..5 y una reseña
     * de 0 estrellas por descuido no tiene sentido.
     */
    fun save(vehicleId: String) {
        val current = _uiState.value
        if (current.isSaving) return
        if (current.rating !in 1..5) {
            _uiState.update { it.copy(errorMessage = "Selecciona una calificación de 1 a 5 estrellas") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            val trimmed = current.experience.trim()
            val result = current.reviewId?.let { reviewId ->
                // Al editar se manda el texto tal cual, incluso vacío: si se
                // mandara null, Gson omitiría el campo y el backend (que solo
                // toca los campos presentes) dejaría el comentario anterior,
                // así que no habría forma de borrarlo.
                reviewRepository.updateReview(
                    reviewId = reviewId,
                    rating = current.rating,
                    comment = trimmed,
                    keepImageUrls = current.existingImageUrls,
                    newImageUris = current.newPhotoUris
                )
            } ?: reviewRepository.createReview(
                // El autor lo pone el repositorio: es el usuario autenticado.
                // El repositorio también avisa a quienes ya reseñaron el carro.
                vehicleId = vehicleId,
                rating = current.rating,
                // Al crear sí conviene null: deja la columna nula en vez de
                // guardar una cadena vacía.
                comment = trimmed.ifBlank { null },
                // Ya resuelta al entrar a la pantalla (o quitada por el
                // usuario): publicar no espera al GPS. Si es null, la reseña
                // se publica igual y solo no sale en el mapa. Al editar no se
                // toca: cambiar el texto no cambia dónde se publicó.
                location = current.capturedLocation,
                imageUris = current.newPhotoUris
            )

            _uiState.update { state ->
                result.fold(
                    onSuccess = { state.copy(isSaving = false, isSaved = true) },
                    onFailure = { error ->
                        state.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "No se pudo guardar la reseña"
                        )
                    }
                )
            }
        }
    }
}
