package com.example.motormates.data.repository

import com.example.motormates.data.source.CarCatalogParser
import com.example.motormates.data.source.LocalCarCatalogDataSource
import com.example.motormates.data.source.RemoteCarCatalogDataSource
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.File
import java.io.IOException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CarCatalogRepositoryImplTest {

    private val local: LocalCarCatalogDataSource = mockk()
    private val remote: RemoteCarCatalogDataSource = mockk()
    private val repository = CarCatalogRepositoryImpl(local, remote)

    private val catalog = mapOf(
        "Renault" to listOf("Duster", "Sandero"),
        "Mazda" to listOf("3", "CX-5")
    )

    @Test
    fun `las marcas salen de la lista local en orden alfabetico`() = runTest {
        coEvery { local.load() } returns catalog

        assertThat(repository.brands()).containsExactly("Mazda", "Renault").inOrder()
    }

    @Test
    fun `junta locales y vPIC sin repetir, los locales primero`() = runTest {
        coEvery { local.load() } returns catalog
        coEvery { remote.models("Mazda", 2020) } returns listOf("CX-5", "Mazda3", "MX-5", "cx-5")

        val models = repository.models("mazda", 2020)

        assertThat(models.map { it.name }).containsExactly("3", "CX-5", "Mazda3", "MX-5").inOrder()
        assertThat(models.map { it.source }).containsExactly(
            CarModelSource.LOCAL, CarModelSource.LOCAL, CarModelSource.VPIC, CarModelSource.VPIC
        ).inOrder()
    }

    @Test
    fun `si vPIC falla quedan los modelos locales`() = runTest {
        coEvery { local.load() } returns catalog
        coEvery { remote.models(any(), any()) } throws IOException("sin red")

        assertThat(repository.models("Renault", 2024).map { it.name }).containsExactly("Duster", "Sandero")
    }

    @Test
    fun `si vPIC no responde a tiempo quedan los locales`() = runTest {
        coEvery { local.load() } returns catalog
        coEvery { remote.models(any(), any()) } coAnswers { awaitCancellation() }

        assertThat(repository.models("Renault", 2024).map { it.name }).containsExactly("Duster", "Sandero")
    }

    @Test
    fun `una marca que solo conoce vPIC tambien funciona y se guarda en cache`() = runTest {
        coEvery { local.load() } returns catalog
        coEvery { remote.models("Honda", 2021) } returns listOf("Civic", "HR-V")

        repository.models("Honda", 2021)
        val again = repository.models("Honda", 2021)

        assertThat(again.map { it.name }).containsExactly("Civic", "HR-V")
        coVerify(exactly = 1) { remote.models("Honda", 2021) }
    }

    @Test
    fun `el archivo real del catalogo es valido y trae marcas que vPIC no tiene`() {
        val json = File("src/main/assets/car_models_co.json").readText()

        val parsed = CarCatalogParser.parse(json)

        assertThat(parsed.keys).containsAtLeast("Renault", "BYD", "Chevrolet", "Mazda")
        assertThat(parsed.getValue("Renault")).contains("Duster")
        assertThat(parsed.values.all { it.isNotEmpty() }).isTrue()
        // Sin repetidos dentro de cada marca.
        parsed.values.forEach { models -> assertThat(models).containsNoDuplicates() }
    }
}
