package com.example.motormates.data.repository

import com.example.motormates.data.model.Station
import com.example.motormates.data.model.StationKind
import com.example.motormates.data.remote.dto.toDomain
import com.example.motormates.data.source.StationDataSource
import java.io.IOException
import javax.inject.Inject
import retrofit2.HttpException

/**
 * Las gasolineras (y, cuando llegue su capa, las de carga eléctrica).
 *
 * Es el único repositorio de la app que no habla con Firestore ni con el
 * backend propio, sino con una API de Google que se paga por llamada. Por eso
 * no expone un Flow como [EventRepository]: no hay nada que escuchar, cada
 * consulta es una petición puntual, y quien decide cuándo vale la pena
 * gastarla es el ViewModel del mapa.
 *
 * Tampoco guarda estado: la guarda de "esta zona ya la consulté" vive en el
 * ViewModel, por la misma razón por la que allá viven la ventana de 24 horas
 * de las reseñas y la de 30 días de los eventos. Es una decisión de producto.
 */
interface StationRepository {

    /**
     * Si la capa se puede usar, es decir si hay API key configurada. El mapa
     * lo consulta para decidir si ofrece el filtro: un chip que siempre falla
     * es peor que un chip que no está.
     */
    fun isAvailable(): Boolean

    suspend fun stationsNear(
        lat: Double,
        lng: Double,
        radiusMeters: Double,
        kind: StationKind
    ): Result<List<Station>>
}

class StationRepositoryImpl @Inject constructor(
    private val stationDataSource: StationDataSource
) : StationRepository {

    override fun isAvailable(): Boolean = stationDataSource.hasApiKey()

    override suspend fun stationsNear(
        lat: Double,
        lng: Double,
        radiusMeters: Double,
        kind: StationKind
    ): Result<List<Station>> =
        try {
            val places = stationDataSource.stationsNear(lat, lng, radiusMeters, kind)
            // Los lugares sin id o sin coordenadas se descartan al mapear, no
            // tumban la consulta entera: una estación mal formada no debe
            // dejar el mapa sin las otras diecinueve.
            Result.success(places.mapNotNull { it.toDomain() })
        } catch (e: IOException) {
            Result.failure(Exception(NO_INTERNET))
        } catch (e: HttpException) {
            Result.failure(Exception(e.toStationMessage(kind)))
        } catch (e: Exception) {
            Result.failure(Exception(loadFailed(kind)))
        }

    /**
     * Los códigos que de verdad devuelve Places, que no son los mismos que los
     * del backend propio (por eso no se reutiliza toSpanishMessage): 403 y 429
     * son los dos fallos normales de una API con llave, y conviene
     * distinguirlos porque se arreglan en sitios distintos — uno en la consola
     * de Google Cloud, el otro esperando o subiendo la cuota.
     */
    private fun HttpException.toStationMessage(kind: StationKind): String = when (code()) {
        403 -> "La API key no tiene permiso para consultar lugares"
        429 -> "Se agotaron las consultas de mapas por ahora"
        in 500..599 -> "El servicio de lugares no está disponible"
        else -> loadFailed(kind)
    }

    /**
     * El mensaje nombra lo que el usuario pidió ver. Decir "no se pudieron
     * cargar las estaciones" con las dos capas encendidas no diría cuál de las
     * dos falló, que es lo único que el usuario puede accionar (apagar esa).
     */
    private fun loadFailed(kind: StationKind): String = when (kind) {
        StationKind.FUEL -> "No se pudieron cargar las gasolineras"
        StationKind.CHARGING -> "No se pudieron cargar los puntos de carga"
    }

    private companion object {
        const val NO_INTERNET = "Sin conexión a internet, verifica tu red"
    }
}
