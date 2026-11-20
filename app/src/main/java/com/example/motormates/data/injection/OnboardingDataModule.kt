package com.example.motormates.data.injection

import com.example.motormates.data.remote.service.VPIC_BASE_URL
import com.example.motormates.data.remote.service.VpicApiService
import com.example.motormates.data.repository.CarCatalogRepository
import com.example.motormates.data.repository.CarCatalogRepositoryImpl
import com.example.motormates.data.repository.PreferencesRepository
import com.example.motormates.data.repository.PreferencesRepositoryImpl
import com.example.motormates.data.source.AssetCarCatalogDataSource
import com.example.motormates.data.source.FirestorePreferencesDataSource
import com.example.motormates.data.source.LocalCarCatalogDataSource
import com.example.motormates.data.source.PreferencesDataSource
import com.example.motormates.data.source.RemoteCarCatalogDataSource
import com.example.motormates.data.source.VpicCarCatalogDataSource
import com.google.gson.Gson
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Onboarding y personalización. Se enlaza directo y no por el
 * pick(firebase, express) de DataSourceModule: las preferencias solo viven
 * en Firestore y el catálogo de carros sale de vPIC y de un archivo local,
 * igual que StationDataModule documenta para Places.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class OnboardingDataModule {

    @Binds
    @Singleton
    abstract fun bindPreferencesDataSource(impl: FirestorePreferencesDataSource): PreferencesDataSource

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository

    @Binds
    @Singleton
    abstract fun bindLocalCarCatalog(impl: AssetCarCatalogDataSource): LocalCarCatalogDataSource

    @Binds
    @Singleton
    abstract fun bindRemoteCarCatalog(impl: VpicCarCatalogDataSource): RemoteCarCatalogDataSource

    @Binds
    @Singleton
    abstract fun bindCarCatalogRepository(impl: CarCatalogRepositoryImpl): CarCatalogRepository
}

@Module
@InstallIn(SingletonComponent::class)
object VpicNetworkModule {

    /**
     * vPIC vive en otro host, así que lleva su propio Retrofit, construido
     * aquí dentro por la misma razón que Places en NetworkProvidersModule (un
     * segundo Retrofit sin calificador sería ambiguo para Hilt). El cliente
     * se deriva del compartido con un tope corto: el autocompletado no puede
     * dejar al usuario esperando.
     */
    @Provides
    @Singleton
    fun provideVpicApiService(client: OkHttpClient, gson: Gson): VpicApiService =
        Retrofit.Builder()
            .baseUrl(VPIC_BASE_URL)
            .client(client.newBuilder().callTimeout(VPIC_TIMEOUT_SECONDS, TimeUnit.SECONDS).build())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(VpicApiService::class.java)

    private const val VPIC_TIMEOUT_SECONDS = 5L
}
