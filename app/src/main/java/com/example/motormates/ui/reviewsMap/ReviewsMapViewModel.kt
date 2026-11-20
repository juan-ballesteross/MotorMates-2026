package com.example.motormates.ui.reviewsMap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.Event
import com.example.motormates.data.model.Review
import com.example.motormates.data.model.ReviewMapMarkerUi
import com.example.motormates.data.model.areNearbyGroup
import com.example.motormates.data.model.toEventMapMarkerUi
import com.example.motormates.data.model.toReviewMapMarkerUi
import com.example.motormates.data.model.toStationMapMarkerUi
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.EventRepository
import com.example.motormates.data.repository.ReviewRepository
import com.example.motormates.data.repository.StationRepository
import com.example.motormates.data.repository.VehicleRepository
import com.example.motormates.data.source.DeviceLocationDataSource
import com.example.motormates.data.util.TimeSource
import com.example.motormates.data.util.toEpochMillisOrNull
import com.example.motormates.data.util.toEventDateTimeEs
import com.example.motormates.data.util.toEventTimeEs
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
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
    private val eventRepository: EventRepository,
    private val authRepository: AuthRepository,
    private val stationRepository: StationRepository,
    private val deviceLocationDataSource: DeviceLocationDataSource,
    private val timeSource: TimeSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewsMapUiState())
    val uiState: StateFlow<ReviewsMapUiState> = _uiState

    /**
     * Una consulta por capa, para poder apagarlas por separado. Dejar viva la
     * de una capa que no se está mirando no es gratis: la de reseñas es un
     * listener de Firestore que reemite con cada like de cualquier reseña
     * reciente, y la de estaciones son peticiones HTTP por cada movimiento
     * de cámara.
     */
    private val layerJobs = mutableMapOf<MapLayer, Job>()

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
     * Lo último que mandó el listener de eventos, sin recortar. Igual que
     * recentReviews: se guarda para poder recalcular con cada tic del reloj
     * (un evento que termina tiene que salir del mapa solo).
     */
    private var upcomingEvents: List<Event> = emptyList()

    /**
     * La última cámara que reportó la pantalla.
     *
     * Es un StateFlow y no un parámetro suelto porque la capa de estaciones la
     * consumen con freno (ver launchStationsLayer): al ser un flujo con
     * valor actual, prender el chip reemite la posición de ahora mismo y la
     * capa consulta sola, sin esperar a que el usuario mueva el mapa.
     *
     * De paso sale gratis otra guarda: un StateFlow no reemite un valor igual
     * al que ya tiene, así que una parada de cámara que deja el mapa
     * exactamente donde estaba no llega a la capa.
     */
    private val cameraFlow = MutableStateFlow<MapCamera?>(null)

    /**
     * La última consulta buena a Places POR CAPA (ver isSameStationArea).
     *
     * Por capa y no una sola: gasolineras y puntos de carga se consultan por
     * separado, así que una guarda compartida haría que, con las dos capas
     * encendidas, la segunda se saltara su consulta porque la primera ya había
     * marcado la zona — y esa capa se quedaría vacía para siempre.
     */
    private val lastStationQueries = mutableMapOf<MapLayer, StationQuery>()

    private data class StationQuery(
        val lat: Double,
        val lng: Double,
        val radiusMeters: Double,
        val atMillis: Long
    )

    init {
        // Las capas de estaciones solo se ofrecen si hay API key configurada:
        // un chip que siempre falla es peor que un chip que no está. Las dos
        // salen de la misma llave, así que se ofrecen o no se ofrecen juntas.
        if (stationRepository.isAvailable()) {
            val stationLayers = MapLayer.entries.filter { it.stationKind != null }
            _uiState.update { it.copy(availableLayers = it.availableLayers + stationLayers) }
        }
    }

    /**
     * Abre (o reabre) las consultas de las capas encendidas. Se vuelve a
     * llamar cada vez que se entra a la pestaña del mapa, y eso importa: la
     * fecha de corte de las reseñas se calcula al abrir, así que reabrir es
     * lo que mantiene la ventana de 24 horas fresca en vez de congelada en el
     * momento en que se creó el ViewModel.
     */
    fun start() {
        _uiState.value.layers.forEach { restartLayer(it) }
    }

    fun retry() = start()

    // ---------- Capas ----------

    /**
     * Prende o apaga una capa. Prenderla abre su consulta; apagarla la
     * cancela y borra sus marcadores, para no dejar escuchando algo que no
     * se está mirando.
     */
    fun toggleLayer(layer: MapLayer) {
        val active = _uiState.value.layers
        val next = if (layer in active) active - layer else active + layer
        _uiState.update { it.copy(layers = next) }
        if (layer in next) restartLayer(layer) else stopLayer(layer)
    }

    /**
     * Reabre la consulta de una capa. A diferencia de toggleLayer, esto
     * SIEMPRE reinicia y nunca es idempotente: reabrir es justo lo que
     * mantiene fresca la ventana de 24 horas al volver a la pestaña (ver el
     * KDoc de refreshMarkers). Si se guardara contra "ya está activa", la
     * ventana se congelaría en el momento en que se creó el ViewModel.
     */
    private fun restartLayer(layer: MapLayer) {
        layerJobs.remove(layer)?.cancel()

        // Se marca como cargando antes de abrir la consulta: si se hiciera
        // después, la primera emisión podría llegar primero y la capa
        // quedaría marcada como cargando para siempre.
        val job = when (layer) {
            MapLayer.REVIEWS -> {
                _uiState.update { it.copy(loadingLayers = it.loadingLayers + layer, errorMessage = null) }
                launchReviewsLayer()
            }

            MapLayer.EVENTS -> {
                _uiState.update { it.copy(loadingLayers = it.loadingLayers + layer, errorMessage = null) }
                launchEventsLayer()
            }

            // Sin marca de cargando acá, al contrario que las otras dos: la
            // consulta de estaciones puede decidir no salir (zoom corto, zona
            // ya consultada), y marcarla antes dejaría la barra de progreso
            // encendida para siempre. La marca la pone maybeFetchStations,
            // justo antes de la petición.
            MapLayer.FUEL_STATIONS,
            MapLayer.CHARGING_STATIONS -> launchStationsLayer(layer)
        } ?: return

        layerJobs[layer] = job
    }

    private fun stopLayer(layer: MapLayer) {
        layerJobs.remove(layer)?.cancel()
        // Apagar y volver a prender el chip tiene que consultar de nuevo,
        // aunque la zona siga fresca: el usuario acaba de pedir verlas.
        lastStationQueries.remove(layer)
        clearLayerData(layer)
    }

    /** Apagar una capa borra sus marcadores y cierra lo que tuviera abierto. */
    private fun clearLayerData(layer: MapLayer) {
        _uiState.update { state ->
            // La tarjeta abierta solo se cierra si era de esta capa: apagar
            // las estaciones no debe cerrar la reseña que se está leyendo.
            val base = state.copy(
                loadingLayers = state.loadingLayers - layer,
                selection = state.selection?.takeIf { it.layer != layer }
            )
            when (layer) {
                MapLayer.REVIEWS -> base.copy(
                    markers = emptyList(),
                    clusterSelectionIds = emptyList(),
                    errorMessage = null
                )

                MapLayer.EVENTS -> base.copy(
                    eventMarkers = emptyList(),
                    eventClusterIds = emptyList(),
                    errorMessage = null
                )

                // Las dos de estaciones se limpian igual: se quita su entrada
                // del mapa y la otra capa, si está encendida, no se entera.
                MapLayer.FUEL_STATIONS,
                MapLayer.CHARGING_STATIONS -> base.copy(
                    stationsByLayer = state.stationsByLayer - layer,
                    stationsMessage = null
                )
            }
        }
    }

    private fun launchReviewsLayer(): Job =
        viewModelScope.launch {
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
                        _uiState.update {
                            it.copy(
                                loadingLayers = it.loadingLayers - MapLayer.REVIEWS,
                                errorMessage = null
                            )
                        }
                        refreshMarkers()
                    },
                    onFailure = { error ->
                        recentReviews = emptyList()
                        _uiState.update {
                            it.copy(
                                loadingLayers = it.loadingLayers - MapLayer.REVIEWS,
                                markers = emptyList(),
                                errorMessage = error.message ?: "No se pudo cargar el mapa de reseñas"
                            )
                        }
                    }
                )
            }
        }

    /**
     * Los eventos próximos. La ventana se decide aquí y no en el repositorio,
     * igual que el corte de 24 horas de las reseñas: es una decisión de
     * producto. Empieza un rato antes de ahora para que un evento en curso
     * siga en el mapa.
     */
    private fun launchEventsLayer(): Job =
        viewModelScope.launch {
            launch {
                // Mismo tic que las reseñas: un evento que termina con el mapa
                // abierto tiene que salir solo, sin recargar.
                timeSource.ticks(TICK_MS).collect { refreshEventMarkers() }
            }

            val now = timeSource.nowMillis()
            eventRepository.observeEventsBetween(
                fromMillis = now - EVENT_WINDOW_PAST_MS,
                toMillis = now + EVENT_WINDOW_FUTURE_MS
            ).collect { result ->
                result.fold(
                    onSuccess = { events ->
                        upcomingEvents = events
                        _uiState.update {
                            it.copy(
                                loadingLayers = it.loadingLayers - MapLayer.EVENTS,
                                errorMessage = null
                            )
                        }
                        refreshEventMarkers()
                    },
                    onFailure = { error ->
                        upcomingEvents = emptyList()
                        _uiState.update {
                            it.copy(
                                loadingLayers = it.loadingLayers - MapLayer.EVENTS,
                                eventMarkers = emptyList(),
                                errorMessage = error.message ?: "No se pudieron cargar los eventos"
                            )
                        }
                    }
                )
            }
        }

    /**
     * El filtro que la consulta no puede hacer.
     *
     * Firestore ya acotó por `startAt`, que es lo que evita traerse la
     * colección entera. Lo que queda se recorta aquí: los que ya terminaron
     * desaparecen, y eso NO se puede pedir a la consulta porque el fin está
     * en otro campo (`endAt`) y filtrar por dos campos exigiría un índice
     * compuesto, que este proyecto evita a propósito.
     *
     * Los cancelados se conservan: quien dijo que iba necesita verlos.
     */
    private fun refreshEventMarkers() {
        val now = timeSource.nowMillis()
        val currentUserId = authRepository.getCurrentUser()?.id
        val today = LocalDate.now()

        val markers = upcomingEvents
            .map { event ->
                event.toEventMapMarkerUi(
                    currentUserId = currentUserId,
                    nowMillis = now,
                    fechaTexto = event.startAt.toEventDateTimeEs(today = today),
                    horarioTexto = event.endAt?.let { fin -> "hasta ${fin.toEventTimeEs()}" }
                )
            }
            .filterNot { it.hasFinished }

        _uiState.update { it.copy(eventMarkers = markers) }
    }

    // ---------- Estaciones (gasolineras y puntos de carga) ----------

    /**
     * Las estaciones de la zona que se está mirando.
     *
     * Una sola implementación para las dos capas: lo único que cambia entre
     * gasolineras y puntos de carga es el [MapLayer.stationKind] que se le pide
     * a Places. Cada capa corre su propio Job, así que prender las dos abre dos
     * consultas independientes, con su propio freno y su propia guarda de zona.
     *
     * Al contrario que las otras dos capas del mapa, acá no hay nada que
     * escuchar: no es un listener que reemite, sino una petición HTTP que se
     * paga por llamada. Por eso estas capas se cuelgan de la cámara y no del
     * reloj.
     *
     * collectLatest + delay es el freno, y hace dos cosas de una: mientras el
     * usuario arrastra, cada posición nueva cancela la espera de la anterior Y
     * la consulta en curso si ya había salido. Un arrastre largo cuesta UNA
     * llamada por capa, no una por cada parada intermedia.
     */
    private fun launchStationsLayer(layer: MapLayer): Job =
        viewModelScope.launch {
            cameraFlow.filterNotNull().collectLatest { camera ->
                delay(STATIONS_DEBOUNCE_MS)
                maybeFetchStations(layer, camera)
            }
        }

    /**
     * Consulta Places, si vale la pena.
     *
     * Cada salida temprana es una llamada que no se cobra, y por eso están
     * todas juntas acá y no repartidas: el orden importa (lo más barato
     * primero) y así se lee de un tirón qué hace falta para que la capa gaste.
     *
     * Si falla no se guarda la zona como consultada: un error no debe bloquear
     * el reintento del siguiente movimiento. Y los marcadores que ya estaban
     * en pantalla se conservan, porque son mejores que ninguno.
     */
    private suspend fun maybeFetchStations(layer: MapLayer, camera: MapCamera) {
        val kind = layer.stationKind ?: return

        // El mapa todavía no se midió, así que no se sabe cuánto abarca.
        if (camera.radiusMeters <= 0.0) return

        // El aviso de "acércate" no se guarda: lo deriva el estado del zoom y
        // de las capas encendidas. Acá solo se vacía lo que hubiera.
        if (camera.zoom < MIN_STATIONS_ZOOM) {
            _uiState.update {
                it.copy(
                    stationsByLayer = it.stationsByLayer - layer,
                    loadingLayers = it.loadingLayers - layer
                )
            }
            return
        }

        if (isSameStationArea(layer, camera)) return

        _uiState.update { it.copy(loadingLayers = it.loadingLayers + layer) }

        stationRepository.stationsNear(
            lat = camera.lat,
            lng = camera.lng,
            radiusMeters = camera.radiusMeters,
            kind = kind
        ).fold(
            onSuccess = { stations ->
                lastStationQueries[layer] = StationQuery(
                    lat = camera.lat,
                    lng = camera.lng,
                    radiusMeters = camera.radiusMeters,
                    atMillis = timeSource.nowMillis()
                )
                _uiState.update { state ->
                    state.copy(
                        stationsByLayer = state.stationsByLayer +
                            (layer to stations.map { it.toStationMapMarkerUi(kind) }),
                        stationsMessage = null,
                        loadingLayers = state.loadingLayers - layer
                    )
                }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        stationsMessage = error.message ?: STATIONS_LOAD_FAILED,
                        loadingLayers = it.loadingLayers - layer
                    )
                }
            }
        )
    }

    /**
     * Si la cámara quedó en la misma zona de la última consulta buena, y esa
     * consulta sigue fresca.
     *
     * Hace falta porque la cámara se detiene muchas más veces de las que el
     * usuario cambia de zona: el encuadre inicial, abrir una tarjeta, volver a
     * la pestaña y cualquier arrastre corto emiten una posición nueva. Sin esta
     * guarda, cada una de esas sería una llamada pagada.
     *
     * El umbral va como fracción del radio visible y no en metros fijos: a
     * zoom de barrio moverse 300 m es cambiar de zona, y a zoom de ciudad no.
     */
    private fun isSameStationArea(layer: MapLayer, camera: MapCamera): Boolean {
        val last = lastStationQueries[layer] ?: return false
        if (timeSource.nowMillis() - last.atMillis >= STATIONS_TTL_MS) return false

        val moved = distanceMeters(last.lat, last.lng, camera.lat, camera.lng)
        if (moved > last.radiusMeters * STATIONS_MOVE_FRACTION) return false

        val radiusChange = abs(camera.radiusMeters - last.radiusMeters) / last.radiusMeters
        return radiusChange <= STATIONS_RADIUS_CHANGE_FRACTION
    }

    /**
     * Distancia aproximada en metros, sin tipos del SDK de Maps (que no
     * existen en la JVM donde corren las pruebas).
     *
     * Es la aproximación plana, de sobra para decidir "se movió mucho" en
     * distancias de pocos kilómetros. El coseno corrige que los grados de
     * longitud se angosten al alejarse del ecuador.
     */
    private fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = (lat2 - lat1) * METERS_PER_DEGREE
        val dLng = (lng2 - lng1) * METERS_PER_DEGREE * cos(Math.toRadians((lat1 + lat2) / 2))
        return hypot(dLat, dLng)
    }

    // ---------- Selección ----------

    fun onEventMarkerClick(eventId: String) {
        _uiState.update {
            it.copy(
                selection = MapSelection(MapLayer.EVENTS, eventId),
                eventClusterIds = emptyList()
            )
        }
    }

    /** Mismo criterio que onClusterClick, pero para la capa de eventos. */
    fun onEventClusterClick(eventIds: List<String>): Boolean {
        val members = _uiState.value.eventMarkers.filter { it.id in eventIds }
        val first = members.firstOrNull() ?: return false
        val samePoint = members.all {
            abs(it.lat - first.lat) < SAME_POINT_DEGREES && abs(it.lng - first.lng) < SAME_POINT_DEGREES
        }
        if (!samePoint) return false

        _uiState.update { it.copy(eventClusterIds = members.map { m -> m.id }, selection = null) }
        return true
    }

    fun dismissEventClusterSelection() {
        _uiState.update { it.copy(eventClusterIds = emptyList()) }
    }

    /**
     * El id no basta: la capa dice en qué lista buscar la estación, y las dos
     * capas pueden tener marcadores en pantalla a la vez.
     */
    fun onStationMarkerClick(layer: MapLayer, stationId: String) {
        _uiState.update { it.copy(selection = MapSelection(layer, stationId)) }
    }

    fun onStationsMessageShown() {
        _uiState.update { it.copy(stationsMessage = null) }
    }

    /**
     * Para que la pantalla avise por el snackbar de esta capa. Lo usa el caso
     * de "no hay app de mapas instalada", que solo la pantalla puede detectar
     * porque depende de qué tiene el teléfono.
     */
    fun showStationsMessage(message: String) {
        _uiState.update { it.copy(stationsMessage = message) }
    }

    fun onMarkerClick(markerId: String) {
        _uiState.update {
            it.copy(
                selection = MapSelection(MapLayer.REVIEWS, markerId),
                clusterSelectionIds = emptyList()
            )
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selection = null) }
    }

    /**
     * Tocar un grupo. Si todas sus reseñas están a 30 m o menos entre sí
     * (groupNearby), se abre la lista: acercar la cámara no las separaría lo
     * suficiente para tocarlas una por una. Si están más lejos, devuelve false
     * y la pantalla acerca la cámara para separarlas.
     */
    fun onClusterClick(markerIds: List<String>): Boolean {
        val members = _uiState.value.markers.filter { it.id in markerIds }
        if (!members.areNearbyGroup()) return false

        _uiState.update { it.copy(clusterSelectionIds = members.map { m -> m.id }, selection = null) }
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
        // Alimenta la capa de estaciones. Se avisa siempre, aunque el chip esté
        // apagado: cameraFlow solo guarda la posición, y quien decide si eso se
        // convierte en una consulta es launchStationsLayer, que únicamente
        // existe mientras esa capa está encendida.
        cameraFlow.value = camera
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

        /** Zoom de barrio, igual que el de una sola reseña. */
        const val MY_LOCATION_ZOOM = 14f

        /**
         * Ventana de eventos: desde hace 6 horas, para que uno en curso siga
         * visible, hasta 30 días adelante, que es hasta donde la gente
         * planea. Es el equivalente del corte de 24 horas de las reseñas.
         */
        const val EVENT_WINDOW_PAST_MS = 6L * 60 * 60 * 1000
        const val EVENT_WINDOW_FUTURE_MS = 30L * 24 * 60 * 60 * 1000

        /** Lo que se espera a que el usuario suelte el mapa antes de consultar. */
        const val STATIONS_DEBOUNCE_MS = 600L

        /** Una zona ya consultada se da por buena durante este rato. */
        const val STATIONS_TTL_MS = 5L * 60 * 1000

        /**
         * Cuánto tiene que moverse la cámara, como fracción del radio visible,
         * para que valga la pena volver a preguntar.
         */
        const val STATIONS_MOVE_FRACTION = 0.3

        /** Y cuánto tiene que cambiar el radio (o sea el zoom), igual en fracción. */
        const val STATIONS_RADIUS_CHANGE_FRACTION = 0.2

        /** Respaldo si el repositorio no trae mensaje; normalmente sí trae. */
        const val STATIONS_LOAD_FAILED = "No se pudieron cargar las estaciones"

        /** Grados a metros. Aproximado, y de sobra para la guarda de movimiento. */
        const val METERS_PER_DEGREE = 111_320.0

        /** ~1 m: por debajo de esto dos eventos son "el mismo sitio" para el zoom máximo. */
        const val SAME_POINT_DEGREES = 0.00001

        const val LOCATION_UNAVAILABLE =
            "No pudimos obtener tu ubicación. Revisa que la ubicación del teléfono esté activa."
    }
}
