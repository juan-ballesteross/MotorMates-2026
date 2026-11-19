package com.example.motormates.data.repository

import com.example.motormates.data.model.StationKind
import com.example.motormates.data.remote.dto.LatLngDto
import com.example.motormates.data.remote.dto.LocalizedTextDto
import com.example.motormates.data.remote.dto.PlaceDto
import com.example.motormates.data.source.StationDataSource
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * StationRepositoryImpl con la fuente mockeada.
 *
 * Lo que se prueba es lo propio del repositorio: descartar los lugares que no
 * sirven, y traducir cada fallo a un mensaje en español que la UI pueda
 * mostrar tal cual.
 */
class StationRepositoryImplTest {

    private lateinit var dataSource: StationDataSource
    private lateinit var repository: StationRepositoryImpl

    @Before
    fun setUp() {
        dataSource = mockk()
        every { dataSource.hasApiKey() } returns true
        repository = StationRepositoryImpl(dataSource)
    }

    @Test
    fun `isAvailable sigue a la fuente`() {
        every { dataSource.hasApiKey() } returns false
        assertThat(repository.isAvailable()).isFalse()

        every { dataSource.hasApiKey() } returns true
        assertThat(repository.isAvailable()).isTrue()
    }

    @Test
    fun `mapea los lugares completos`() = runTest {
        coEvery { dataSource.stationsNear(any(), any(), any(), any()) } returns listOf(
            place(id = "p1", name = "Terpel 85", address = "Calle 85 #15-20")
        )

        val stations = repository.stationsNear(4.67, -74.05, 2_000.0, StationKind.FUEL).getOrThrow()

        assertThat(stations).hasSize(1)
        assertThat(stations.first().id).isEqualTo("p1")
        assertThat(stations.first().name).isEqualTo("Terpel 85")
        assertThat(stations.first().address).isEqualTo("Calle 85 #15-20")
        assertThat(stations.first().location.lat).isEqualTo(4.67)
    }

    /**
     * Un lugar mal formado no tumba la consulta entera: el mapa se queda con
     * los que sí sirven en vez de quedarse sin ninguno.
     */
    @Test
    fun `descarta los lugares sin id y sin coordenadas, y conserva el resto`() = runTest {
        coEvery { dataSource.stationsNear(any(), any(), any(), any()) } returns listOf(
            place(id = null),
            place(id = "  "),
            place(id = "sin-punto", location = null),
            place(id = "bueno")
        )

        val stations = repository.stationsNear(4.67, -74.05, 2_000.0, StationKind.FUEL).getOrThrow()

        assertThat(stations.map { it.id }).containsExactly("bueno")
    }

    /** Nombre y dirección sí pueden faltar: el respaldo lo pone el mapper a la UI. */
    @Test
    fun `un lugar sin nombre ni direccion igual genera estacion`() = runTest {
        coEvery { dataSource.stationsNear(any(), any(), any(), any()) } returns listOf(
            place(id = "p1", name = "   ", address = "")
        )

        val station = repository.stationsNear(4.67, -74.05, 2_000.0, StationKind.FUEL)
            .getOrThrow()
            .single()

        assertThat(station.name).isNull()
        assertThat(station.address).isNull()
    }

    @Test
    fun `sin red devuelve el mensaje de conexion`() = runTest {
        coEvery { dataSource.stationsNear(any(), any(), any(), any()) } throws IOException("boom")

        val result = repository.stationsNear(4.67, -74.05, 2_000.0, StationKind.FUEL)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message)
            .isEqualTo("Sin conexión a internet, verifica tu red")
    }

    /**
     * 403 y 429 se distinguen a propósito: uno se arregla en la consola de
     * Google Cloud y el otro esperando, así que decir "no se pudo" en los dos
     * casos dejaría al usuario sin saber qué pasó.
     */
    @Test
    fun `un 403 avisa que la llave no tiene permiso`() = runTest {
        coEvery { dataSource.stationsNear(any(), any(), any(), any()) } throws httpError(403)

        val result = repository.stationsNear(4.67, -74.05, 2_000.0, StationKind.FUEL)

        assertThat(result.exceptionOrNull()?.message)
            .isEqualTo("La API key no tiene permiso para consultar lugares")
    }

    @Test
    fun `un 429 avisa que se agotaron las consultas`() = runTest {
        coEvery { dataSource.stationsNear(any(), any(), any(), any()) } throws httpError(429)

        val result = repository.stationsNear(4.67, -74.05, 2_000.0, StationKind.FUEL)

        assertThat(result.exceptionOrNull()?.message)
            .isEqualTo("Se agotaron las consultas de mapas por ahora")
    }

    @Test
    fun `un 500 avisa que el servicio no esta disponible`() = runTest {
        coEvery { dataSource.stationsNear(any(), any(), any(), any()) } throws httpError(503)

        val result = repository.stationsNear(4.67, -74.05, 2_000.0, StationKind.FUEL)

        assertThat(result.exceptionOrNull()?.message)
            .isEqualTo("El servicio de lugares no está disponible")
    }

    /** Cualquier otra cosa no debe filtrar el texto crudo de la librería. */
    @Test
    fun `un error inesperado cae en el mensaje generico`() = runTest {
        coEvery { dataSource.stationsNear(any(), any(), any(), any()) } throws
            IllegalStateException("NullPointerException en el parser")

        val result = repository.stationsNear(4.67, -74.05, 2_000.0, StationKind.FUEL)

        assertThat(result.exceptionOrNull()?.message)
            .isEqualTo("No se pudieron cargar las gasolineras")
    }

    /**
     * El mensaje nombra lo que el usuario pidió: con las dos capas encendidas,
     * "no se pudieron cargar las estaciones" no diría cuál de las dos falló.
     */
    @Test
    fun `el mensaje de fallo nombra la clase de estacion`() = runTest {
        coEvery { dataSource.stationsNear(any(), any(), any(), any()) } throws httpError(418)

        val gasolineras = repository.stationsNear(4.67, -74.05, 2_000.0, StationKind.FUEL)
        val carga = repository.stationsNear(4.67, -74.05, 2_000.0, StationKind.CHARGING)

        assertThat(gasolineras.exceptionOrNull()?.message)
            .isEqualTo("No se pudieron cargar las gasolineras")
        assertThat(carga.exceptionOrNull()?.message)
            .isEqualTo("No se pudieron cargar los puntos de carga")
    }

    /** El tipo pedido llega tal cual a la fuente: es lo único que separa las capas. */
    @Test
    fun `pasa el tipo de estacion a la fuente`() = runTest {
        coEvery { dataSource.stationsNear(any(), any(), any(), any()) } returns emptyList()

        repository.stationsNear(4.67, -74.05, 2_000.0, StationKind.CHARGING)

        coVerify { dataSource.stationsNear(4.67, -74.05, 2_000.0, StationKind.CHARGING) }
    }

    private fun place(
        id: String?,
        name: String? = "Terpel",
        address: String? = "Calle 85",
        location: LatLngDto? = LatLngDto(latitude = 4.67, longitude = -74.05)
    ) = PlaceDto(
        id = id,
        displayName = name?.let { LocalizedTextDto(text = it) },
        formattedAddress = address,
        location = location
    )

    private fun httpError(code: Int) = HttpException(
        Response.error<Any>(code, "".toResponseBody("application/json".toMediaType()))
    )
}
