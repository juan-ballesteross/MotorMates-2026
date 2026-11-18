package com.example.motormates.ui.reviewsMap

import com.example.motormates.data.model.EventMapMarkerUi
import com.example.motormates.data.model.ReviewMapMarkerUi
import com.example.motormates.data.model.StationKind
import com.example.motormates.data.model.StationMapMarkerUi

/**
 * Lo que necesita el mapa de reseñas recientes.
 *
 * Del marcador abierto se guarda el id y no el objeto: el listener de
 * Firestore reemite con cada like o cada reseña nueva, y si se guardara la
 * copia quedaría desactualizada. Con el id, selectedMarker siempre resuelve
 * contra la lista vigente, y si la reseña desaparece la tarjeta se cierra
 * sola. Lo mismo vale para el grupo abierto (clusterSelectionIds).
 */
data class ReviewsMapUiState(
    val markers: List<ReviewMapMarkerUi> = emptyList(),
    val eventMarkers: List<EventMapMarkerUi> = emptyList(),
    /** Eventos de un grupo que el zoom no puede separar. Vacío = hoja cerrada. */
    val eventClusterIds: List<String> = emptyList(),
    /**
     * La tarjeta abierta, o null si no hay ninguna. Lleva la capa además del
     * id por dos razones: dos capas distintas pueden traer ids iguales, y es
     * la capa la que decide qué tarjeta se dibuja.
     */
    val selection: MapSelection? = null,
    /**
     * Capas encendidas. Arranca solo con reseñas, que es exactamente lo que
     * el mapa mostraba antes de que existieran los filtros.
     */
    val layers: Set<MapLayer> = setOf(MapLayer.REVIEWS),
    /**
     * Capas que todavía no han entregado su primera emisión. Se guarda el
     * conjunto y no un booleano porque, con varias capas, "cargando"
     * significa "alguna de las encendidas aún no ha llegado".
     */
    val loadingLayers: Set<MapLayer> = emptySet(),
    /**
     * Las capas que se le ofrecen al usuario.
     *
     * No es MapLayer.entries: una capa se ofrece cuando está implementada y
     * cuando se puede consultar. Vive en el estado y no como constante de la
     * pantalla porque lo segundo se decide en tiempo de ejecución — las de
     * estaciones necesitan que haya API key configurada.
     */
    val availableLayers: List<MapLayer> = listOf(MapLayer.REVIEWS, MapLayer.EVENTS),
    val errorMessage: String? = null,
    /** Reseñas de un grupo que el zoom no puede separar (mismo punto). Vacío = hoja cerrada. */
    val clusterSelectionIds: List<String> = emptyList(),
    val locationAccess: LocationAccess = LocationAccess.NOT_REQUESTED,
    val isLocating: Boolean = false,
    /** A dónde mover la cámara por "Mi ubicación". Se consume una vez (consumeMyLocationTarget). */
    val myLocationTarget: MapCamera? = null,
    val locationMessage: String? = null,
    /**
     * La cámara se encuadra sola una única vez. Vive en el ViewModel y no en
     * la pantalla porque el mapa se destruye al cambiar de pestaña y el
     * ViewModel no: así, al volver, no salta otra vez.
     */
    val hasFittedInitially: Boolean = false,
    /** Marcadores que ya estaban en pantalla la última vez que se encuadró. */
    val acknowledgedMarkerIds: Set<String> = emptySet(),
    /** Última posición de la cámara, para restaurarla al volver a la pestaña. */
    val savedCamera: MapCamera? = null,
    /**
     * Los marcadores de estaciones, separados por capa.
     *
     * Un mapa y no una lista por capa (stations + chargingStations) porque las
     * dos se tratan igual en todo el recorrido: misma consulta, mismo marcador,
     * misma tarjeta. Con el mapa, agregar una tercera clase de estación no
     * añade un campo al estado ni una rama a clearLayerData.
     *
     * Las claves son siempre capas con [MapLayer.stationKind] no nulo.
     */
    val stationsByLayer: Map<MapLayer, List<StationMapMarkerUi>> = emptyMap(),
    /**
     * El fallo de la capa de estaciones, para mostrarlo en un snackbar.
     *
     * NO va en [errorMessage] a propósito: ese pinta un aviso a pantalla
     * completa sobre el mapa. Si una consulta de Places falla mientras el
     * usuario arrastra el mapa, taparía los marcadores de reseñas y de
     * eventos que sí cargaron, por un fallo de una capa que es un añadido.
     */
    val stationsMessage: String? = null
) {
    /** Alguna capa encendida sigue esperando su primera emisión. */
    val isLoading: Boolean
        get() = loadingLayers.isNotEmpty()

    /** El id de la reseña abierta, que es lo que necesita la capa de marcadores. */
    val selectedMarkerId: String?
        get() = selection?.takeIf { it.layer == MapLayer.REVIEWS }?.id

    val selectedMarker: ReviewMapMarkerUi?
        get() = markers.firstOrNull { it.id == selectedMarkerId }

    val selectedEvent: EventMapMarkerUi?
        get() = selection?.takeIf { it.layer == MapLayer.EVENTS }
            ?.let { sel -> eventMarkers.firstOrNull { it.id == sel.id } }

    val eventClusterSelection: List<EventMapMarkerUi>
        get() = eventClusterIds.mapNotNull { id -> eventMarkers.firstOrNull { it.id == id } }

    /** Vale para las dos capas de estaciones: se busca en la que abrió la tarjeta. */
    val selectedStation: StationMapMarkerUi?
        get() = selection?.takeIf { it.layer.stationKind != null }
            ?.let { sel -> stationsByLayer[sel.layer]?.firstOrNull { it.id == sel.id } }

    fun stationsFor(layer: MapLayer): List<StationMapMarkerUi> =
        stationsByLayer[layer].orEmpty()

    /** Las capas de estaciones encendidas, que son las que consultan y se pintan. */
    val activeStationLayers: List<MapLayer>
        get() = MapLayer.entries.filter { it.stationKind != null && it in layers }

    /**
     * La cámara está demasiado lejos para que las estaciones tengan sentido.
     *
     * Se deriva en vez de guardarse: depende solo del zoom y de qué capas están
     * encendidas, que ya están acá. Guardarlo obligaba a recordar limpiarlo al
     * apagar una capa, y apagar una de las dos dejaba el aviso mintiendo sobre
     * la otra.
     */
    val isStationsZoomTooLow: Boolean
        get() = activeStationLayers.isNotEmpty() &&
            savedCamera?.let { it.radiusMeters > 0 && it.zoom < MIN_STATIONS_ZOOM } == true

    val clusterSelection: List<ReviewMapMarkerUi>
        get() = clusterSelectionIds.mapNotNull { id -> markers.firstOrNull { it.id == id } }

    /** Reseñas que llegaron después del último encuadre: alimenta el chip "N nuevas". */
    val newMarkersCount: Int
        get() = if (!hasFittedInitially) 0 else markers.count { it.id !in acknowledgedMarkerIds }

    /**
     * Mapa vacío de verdad: ni cargando, ni con error, y sin marcadores de
     * NINGUNA capa. Si solo una capa está vacía el mapa no está vacío, y
     * mostrar el aviso taparía los marcadores de las demás.
     */
    val isEmpty: Boolean
        get() = markers.isEmpty() && eventMarkers.isEmpty() &&
            stationsByLayer.values.all { it.isEmpty() } &&
            !isLoading && errorMessage == null
}

/**
 * En qué punto está el permiso de ubicación. El mapa funciona en todos los
 * casos: la ubicación solo prende el punto azul y el botón "Mi ubicación".
 */
enum class LocationAccess {
    GRANTED,
    /** Nunca se ha pedido: al tocar "Mi ubicación" se explica y se pide. */
    NOT_REQUESTED,
    /** Lo negó, pero Android todavía deja volver a preguntar. */
    DENIED_CAN_ASK,
    /** "No volver a preguntar": la única salida es Ajustes. */
    DENIED_PERMANENTLY,
    /** Hay permiso pero la ubicación del teléfono está apagada. */
    SERVICES_OFF,
    /** Sin Google Play Services: no hay cómo ubicar, se oculta el botón. */
    UNAVAILABLE
}

/**
 * Posición de la cámara sin tipos del SDK de Maps, para que el ViewModel se
 * pruebe en la JVM.
 *
 * [radiusMeters] es cuánto abarca la pantalla: la distancia del centro a la
 * esquina de la región visible. Hace falta para poder preguntarle a Places
 * "qué hay en lo que estoy mirando", y se calcula en la pantalla porque solo
 * allá se conoce el tamaño de la vista. Vale 0 mientras el mapa no se haya
 * medido, y el ViewModel lo trata como "todavía no sé, no consultes".
 */
data class MapCamera(
    val lat: Double,
    val lng: Double,
    val zoom: Float,
    val radiusMeters: Double = 0.0
)

/**
 * Qué marcador está abierto.
 *
 * Se guarda la capa y el id, no el objeto, por la misma razón de siempre (ver
 * el KDoc de ReviewsMapUiState): los listeners reemiten y una copia quedaría
 * desactualizada. Al resolverse contra la lista viva, un marcador que
 * desaparece cierra su propia tarjeta.
 *
 * Es un data class y no una interfaz sellada por tipo: todos los casos
 * cargarían el mismo único String y producirían un `when` idéntico, así que
 * serían tipos nuevos a cambio de nada.
 */
data class MapSelection(val layer: MapLayer, val id: String)

/**
 * Las capas que el mapa puede mostrar a la vez.
 *
 * Son valores hoja y no una jerarquía ("ESTACIONES" con sub-tipos) a
 * propósito: con hojas no existe el estado inválido "estaciones encendidas
 * pero sin ningún sub-tipo", y cada valor corresponde exactamente a una
 * consulta y a una capa de Clustering. El Set + toggle es el mismo patrón de
 * ReviewAspectChips, la única selección múltiple que ya había en la app.
 */
enum class MapLayer(val stationKind: StationKind? = null) {
    /** Reseñas de las últimas 24 horas: la capa que existía desde el principio. */
    REVIEWS,
    EVENTS,
    /**
     * Las dos de estaciones llevan su [StationKind] dentro, y eso es lo que las
     * hace una sola implementación: el tipo es lo ÚNICO que cambia entre
     * consultar gasolineras y consultar puntos de carga, así que el ViewModel
     * recorre las capas con stationKind no nulo en vez de tener una rama por
     * cada una. Para las otras capas es null, que es justo la pregunta
     * "¿es una capa de estaciones?".
     */
    FUEL_STATIONS(StationKind.FUEL),
    CHARGING_STATIONS(StationKind.CHARGING)
}

/**
 * Por debajo de este zoom no se consultan estaciones.
 *
 * No es solo por ahorrar: con el tope de 20 resultados que impone Places, una
 * consulta a zoom de ciudad devuelve 20 de las cientos que hay y el mapa queda
 * mintiendo. Y cuesta exactamente lo mismo.
 *
 * Vive acá, junto a la definición de qué es una capa de estaciones, porque lo
 * necesitan los dos lados: el ViewModel para decidir si consulta y el estado
 * para derivar el aviso de "acércate".
 */
const val MIN_STATIONS_ZOOM = 13f
