package com.example.motormates.ui.reviewsMap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.Review
import com.example.motormates.data.model.ReviewMapMarkerUi
import com.example.motormates.data.model.toReviewMapMarkerUi
import com.example.motormates.data.repository.ReviewRepository
import com.example.motormates.data.repository.VehicleRepository
import com.example.motormates.data.source.DeviceLocationDataSource
import com.example.motormates.data.util.TimeSource
import com.example.motormates.data.util.toEpochMillisOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.abs
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
    private val reviewRepository: ReviewRepository,
    private val vehicleRepository: VehicleRepository,
    private val deviceLocationDataSource: DeviceLocationDataSource,
    private val timeSource: TimeSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewsMapUiState())
    val uiState: StateFlow<ReviewsMapUiState> = _uiState

    private var markersJob: Job? = null

    /**
     * Lo último que mandó el listener, sin filtrar. Se guarda para poder
     * recalcular los marcadores con cada tic del reloj, no solo cuando
     * Firestore reemite.
     */
    private var recentReviews: List<Review> = emptyList()

    /**
     * vehicleId -> foto del carro. Null = todavía no se pudo cargar el
     * catálogo (se reintenta al volver a entrar). Se carga una sola vez: el
     * catálogo es corto y no cambia mientras se mira el mapa.
     */
    private var vehicleImages: Map<String, String?>? = null

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

            // En paralelo con el listener: la foto es un adorno y no debe
            // retrasar los marcadores. Si falla, salen con el ícono de respaldo.
            if (vehicleImages == null) {
                launch {
                    vehicleRepository.getVehicles().onSuccess { vehicles ->
                        vehicleImages = vehicles.associate { it.id to it.imageUrl }
                        refreshMarkers()
                    }
                }
            }

            // Sin esto, una reseña que cumple 24 horas con el mapa abierto
            // se quedaría pegada y el "hace 3 min" no avanzaría.
            launch {
                timeSource.ticks(TICK_MS).collect { refreshMarkers() }
            }

            reviewRepository.observeRecentReviews(sinceMillis = recentWindowStart()).collect { result ->
                result.fold(
                    onSuccess = { reviews ->
                        recentReviews = reviews
                        _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                        refreshMarkers()
                    },
                    onFailure = { error ->
                        recentReviews = emptyList()
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                markers = emptyList(),
                                errorMessage = error.message ?: "No se pudo cargar el mapa de reseñas"
                            )
                        }
                    }
                )
            }
        }
    }

    fun retry() = start()

    // ---------- Selección ----------

    fun onMarkerClick(markerId: String) {
        _uiState.update { it.copy(selectedMarkerId = markerId, clusterSelectionIds = emptyList()) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedMarkerId = null) }
    }

    /**
     * Un grupo que se puede separar se abre con zoom (lo hace la pantalla).
     * Uno cuyas reseñas están todas en el mismo punto, por ejemplo un
     * centro comercial, no se separa con ningún zoom: ese abre la lista.
     *
     * @return true si se abrió la lista; false si la pantalla debe acercar.
     */
    fun onClusterClick(markerIds: List<String>): Boolean {
        val members = _uiState.value.markers.filter { it.id in markerIds }
        val first = members.firstOrNull() ?: return false
        val samePoint = members.all {
            abs(it.lat - first.lat) < SAME_POINT_DEGREES && abs(it.lng - first.lng) < SAME_POINT_DEGREES
        }
        if (!samePoint) return false

        _uiState.update { it.copy(clusterSelectionIds = members.map { m -> m.id }, selectedMarkerId = null) }
        return true
    }

    fun dismissClusterSelection() {
        _uiState.update { it.copy(clusterSelectionIds = emptyList()) }
    }

    // ---------- Cámara ----------

    /** La pantalla avisa que encuadró: desde aquí cuentan las "reseñas nuevas". */
    fun onCameraFitted() {
        _uiState.update { state ->
            state.copy(hasFittedInitially = true, acknowledgedMarkerIds = state.markers.map { it.id }.toSet())
        }
    }

    fun onCameraIdle(camera: MapCamera) {
        _uiState.update { it.copy(savedCamera = camera) }
    }

    // ---------- Mi ubicación ----------

    /** Lo informa la pantalla, que es la única que puede consultar permisos y ajustes de Android. */
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
                        myLocationTarget = MapCamera(location.lat, location.lng, MY_LOCATION_ZOOM)
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

    // ---------- Marcadores ----------

    /**
     * Dos recortes, no uno:
     *
     * - La consulta ya pidió solo lo publicado después de la fecha de corte,
     *   que es lo que evita traerse la colección entera.
     * - Aquí se vuelve a filtrar contra la hora actual porque el listener sigue
     *   vivo mientras la pantalla esté abierta.
     *
     * La fecha se compara en millis y no como texto ISO: Instant.toString()
     * escribe los segundos fraccionarios con precisión variable, así que el
     * orden alfabético no coincide con el cronológico.
     */
    private fun refreshMarkers() {
        val cutoff = recentWindowStart()
        val images = vehicleImages.orEmpty()
        val markers = recentReviews
            .filter { review ->
                val createdAt = review.createdAt.toEpochMillisOrNull()
                createdAt != null && createdAt >= cutoff
            }
            // mapNotNull descarta las reseñas sin ubicación: no hay dónde
            // ponerles un marcador.
            .mapNotNull { it.toReviewMapMarkerUi(vehicleImageUrl = images[it.vehicleId]) }

        _uiState.update { it.copy(markers = markers) }
    }

    private fun recentWindowStart(): Long = timeSource.nowMillis() - RECENT_WINDOW_MS

    private companion object {
        const val RECENT_WINDOW_HOURS = 24L
        const val RECENT_WINDOW_MS = RECENT_WINDOW_HOURS * 60 * 60 * 1000

        /** Una vez por minuto: es la resolución del "hace N min". */
        const val TICK_MS = 60_000L

        /** ~1 m: por debajo de esto dos reseñas son "el mismo sitio" para el zoom máximo. */
        const val SAME_POINT_DEGREES = 0.00001

        /** Zoom de barrio, igual que el de una sola reseña. */
        const val MY_LOCATION_ZOOM = 14f

        const val LOCATION_UNAVAILABLE =
            "No pudimos obtener tu ubicación. Revisa que la ubicación del teléfono esté activa."
    }
}
