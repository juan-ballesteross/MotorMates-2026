package com.example.motormates.ui.pickLocation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.ReviewLocation
import com.example.motormates.data.source.DeviceLocationDataSource
import com.example.motormates.data.source.PlaceNameDataSource
import com.example.motormates.ui.reviewsMap.LocationAccess
import com.example.motormates.ui.reviewsMap.MapCamera
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Elegir en el mapa dónde será un evento.
 *
 * Reutiliza las mismas fuentes que el mapa de reseñas: la ubicación del
 * dispositivo para abrir donde está el usuario, y el geocodificador para
 * proponerle un nombre de sitio al punto elegido.
 */
@HiltViewModel
class PickLocationViewModel @Inject constructor(
    private val deviceLocationDataSource: DeviceLocationDataSource,
    private val placeNameDataSource: PlaceNameDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(PickLocationUiState())
    val uiState: StateFlow<PickLocationUiState> = _uiState

    private var started = false

    /**
     * @param lat y [lng] el punto que ya tenía el evento, al editar. Si no
     *   viene, se intenta abrir donde está el usuario; si tampoco, el mapa
     *   usa su centro por defecto.
     */
    fun start(lat: Double?, lng: Double?) {
        if (started) return
        started = true

        if (lat != null && lng != null) {
            _uiState.update { it.copy(initialCamera = MapCamera(lat, lng, PICK_ZOOM)) }
            return
        }

        viewModelScope.launch {
            val location = deviceLocationDataSource.lastKnownLocation() ?: return@launch
            _uiState.update {
                // Solo si el usuario no ha movido ya la cámara: no se le
                // arrebata el mapa por una ubicación que llegó tarde.
                if (it.initialCamera != null) it
                else it.copy(initialCamera = MapCamera(location.lat, location.lng, PICK_ZOOM))
            }
        }
    }

    fun onLocationAccessChanged(access: LocationAccess) {
        _uiState.update {
            it.copy(
                locationAccess = access,
                isLocating = if (access == LocationAccess.GRANTED) it.isLocating else false
            )
        }
    }

    fun centerOnMyLocation() {
        val state = _uiState.value
        if (state.locationAccess != LocationAccess.GRANTED || state.isLocating) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLocating = true, locationMessage = null) }
            val location = deviceLocationDataSource.lastKnownLocation()
            _uiState.update {
                if (location == null) {
                    it.copy(isLocating = false, locationMessage = LOCATION_UNAVAILABLE)
                } else {
                    it.copy(
                        isLocating = false,
                        myLocationTarget = MapCamera(location.lat, location.lng, PICK_ZOOM)
                    )
                }
            }
        }
    }

    fun consumeMyLocationTarget() {
        _uiState.update { it.copy(myLocationTarget = null) }
    }

    fun clearLocationMessage() {
        _uiState.update { it.copy(locationMessage = null) }
    }

    /**
     * Confirma el punto del centro de la cámara. Se le resuelve el nombre del
     * sitio antes de devolverlo, igual que al publicar una reseña, para que
     * el formulario pueda prellenarlo.
     *
     * El geocodificador no bloquea el resultado: si no responde, se devuelve
     * el punto sin nombre (`placeNameFor` nunca lanza y puede dar null).
     */
    fun confirm(lat: Double, lng: Double, onPicked: (ReviewLocation) -> Unit) {
        if (_uiState.value.isResolvingPlace) return

        viewModelScope.launch {
            _uiState.update { it.copy(isResolvingPlace = true) }
            val raw = ReviewLocation(lat = lat, lng = lng)
            val placeName = placeNameDataSource.placeNameFor(raw)
            _uiState.update { it.copy(isResolvingPlace = false) }
            onPicked(raw.copy(placeName = placeName))
        }
    }

    private companion object {
        /** Zoom de manzana: a este nivel la retícula del centro es precisa. */
        const val PICK_ZOOM = 16f

        const val LOCATION_UNAVAILABLE =
            "No pudimos obtener tu ubicación. Revisa que la ubicación del teléfono esté activa."
    }
}
