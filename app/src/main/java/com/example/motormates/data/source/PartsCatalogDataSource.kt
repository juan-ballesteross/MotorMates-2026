package com.example.motormates.data.source

import android.content.Context
import com.example.motormates.data.dto.PartDto
import com.example.motormates.data.dto.PartsCatalogJson
import com.example.motormates.data.dto.TuningPackageDto
import com.example.motormates.data.model.PartsCatalog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/** Catálogo en Firestore (colecciones "parts" y "tuningPackages"), sembrado por seed/seed.js. */
interface RemotePartsCatalogDataSource {
    suspend fun getCatalog(): PartsCatalog
}

/**
 * Catálogo empaquetado en la app (assets/parts_catalog.json). Es el mismo
 * archivo que sube el seeder: sirve de respaldo para que la demo funcione
 * aunque Firestore todavía no tenga las piezas o no haya conexión.
 */
interface LocalPartsCatalogDataSource {
    suspend fun getCatalog(): PartsCatalog
}

class FirestorePartsCatalogDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) : RemotePartsCatalogDataSource {

    override suspend fun getCatalog(): PartsCatalog {
        val parts = firestore.collection(PARTS_COLLECTION).get().await()
            .toObjects(PartDto::class.java)
            .mapNotNull { it.toDomain() }
        val packages = firestore.collection(TUNING_PACKAGES_COLLECTION).get().await()
            .toObjects(TuningPackageDto::class.java)
            .mapNotNull { it.toDomain() }
        return PartsCatalog(parts, packages)
    }
}

class AssetPartsCatalogDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) : LocalPartsCatalogDataSource {

    override suspend fun getCatalog(): PartsCatalog = withContext(Dispatchers.IO) {
        val json = context.assets.open(CATALOG_ASSET).bufferedReader().use { it.readText() }
        PartsCatalogParser.parse(json)
    }

    companion object {
        const val CATALOG_ASSET = "parts_catalog.json"
    }
}

/** Separado para poder probar el archivo real del catálogo en la JVM. */
object PartsCatalogParser {
    private val gson = Gson()

    fun parse(json: String): PartsCatalog =
        gson.fromJson(json, PartsCatalogJson::class.java)?.toDomain()
            ?: PartsCatalog(emptyList(), emptyList())
}
