package com.example.motormates.data.repository

import com.example.motormates.data.model.Part
import com.example.motormates.data.model.PartCategory
import com.example.motormates.data.model.PartsCatalog
import com.example.motormates.data.source.LocalPartsCatalogDataSource
import com.example.motormates.data.source.RemotePartsCatalogDataSource
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class PartsCatalogRepositoryImplTest {

    private lateinit var remote: RemotePartsCatalogDataSource
    private lateinit var local: LocalPartsCatalogDataSource
    private lateinit var repository: PartsCatalogRepositoryImpl

    private fun catalogWith(id: String) = PartsCatalog(
        parts = listOf(Part(id = id, name = id, brand = "", category = PartCategory.RINES, priceMinCop = 1, priceMaxCop = 1)),
        packages = emptyList()
    )

    @Before
    fun setUp() {
        remote = mockk()
        local = mockk()
        repository = PartsCatalogRepositoryImpl(remote, local)
    }

    @Test
    fun `usa Firestore cuando tiene piezas`() = runTest {
        coEvery { remote.getCatalog() } returns catalogWith("remota")

        val catalog = repository.getCatalog().getOrThrow()

        assertThat(catalog.parts.single().id).isEqualTo("remota")
        coVerify(exactly = 0) { local.getCatalog() }
    }

    @Test
    fun `si Firestore esta vacio usa el catalogo empaquetado`() = runTest {
        coEvery { remote.getCatalog() } returns PartsCatalog(emptyList(), emptyList())
        coEvery { local.getCatalog() } returns catalogWith("local")

        assertThat(repository.getCatalog().getOrThrow().parts.single().id).isEqualTo("local")
    }

    @Test
    fun `si Firestore falla usa el catalogo empaquetado`() = runTest {
        coEvery { remote.getCatalog() } throws IOException("sin red")
        coEvery { local.getCatalog() } returns catalogWith("local")

        assertThat(repository.getCatalog().getOrThrow().parts.single().id).isEqualTo("local")
    }

    @Test
    fun `queda en memoria despues de la primera carga`() = runTest {
        coEvery { remote.getCatalog() } returns catalogWith("remota")

        repository.getCatalog()
        repository.getCatalog()

        coVerify(exactly = 1) { remote.getCatalog() }
    }

    @Test
    fun `si ninguno tiene piezas devuelve error`() = runTest {
        coEvery { remote.getCatalog() } returns PartsCatalog(emptyList(), emptyList())
        coEvery { local.getCatalog() } returns PartsCatalog(emptyList(), emptyList())

        assertThat(repository.getCatalog().exceptionOrNull()).hasMessageThat().contains("vacío")
    }
}
