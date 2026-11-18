package com.example.motormates.di

import com.example.motormates.BuildConfig
import com.example.motormates.data.remote.BASE_URL
import com.example.motormates.data.remote.service.PLACES_BASE_URL
import com.example.motormates.data.remote.service.PlacesApiService
import com.example.motormates.data.remote.service.ReviewApiService
import com.example.motormates.data.remote.service.UserApiService
import com.example.motormates.data.remote.service.VehicleApiService
import com.example.motormates.data.repository.ReviewRepository
import com.example.motormates.data.repository.ReviewRepositoryImpl
import com.example.motormates.data.repository.UserRepository
import com.example.motormates.data.repository.UserRepositoryImpl
import com.example.motormates.data.repository.VehicleRepository
import com.example.motormates.data.repository.VehicleRepositoryImpl
import com.example.motormates.data.source.PLACES_API_KEY_NAME
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Mismo esquema que AuthModule: un object con @Provides para lo que no
 * podemos anotar (clases de librerías) y un abstract class con @Binds
 * para amarrar cada interfaz a su implementación.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkProvidersModule {

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
            // BODY imprime también los headers, y por ahí viaja la llave de
            // Places en cada consulta. Sin esto quedaría escrita en logcat.
            redactHeader("X-Goog-Api-Key")
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(logging: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideVehicleApiService(retrofit: Retrofit): VehicleApiService =
        retrofit.create(VehicleApiService::class.java)

    @Provides
    @Singleton
    fun provideUserApiService(retrofit: Retrofit): UserApiService =
        retrofit.create(UserApiService::class.java)

    @Provides
    @Singleton
    fun provideReviewApiService(retrofit: Retrofit): ReviewApiService =
        retrofit.create(ReviewApiService::class.java)

    /**
     * La llave de Places, provista aparte para que nada de lo que se prueba
     * en la JVM tenga que tocar BuildConfig.
     *
     * Puede venir vacía, y eso no es un error: significa que la capa de
     * gasolineras no está configurada, y StationRepository.isAvailable() lo
     * traduce en "no ofrezcas ese filtro".
     */
    @Provides
    @Singleton
    @Named(PLACES_API_KEY_NAME)
    fun providePlacesApiKey(): String = BuildConfig.PLACES_API_KEY

    /**
     * Places vive en otro host, así que necesita su propio Retrofit.
     *
     * Se construye AQUÍ DENTRO y no como un @Provides aparte a propósito: ya
     * hay un Retrofit sin calificador (el del backend propio, arriba), y un
     * segundo binding del mismo tipo rompería la compilación de Hilt por
     * ambigüedad. El OkHttpClient y el Gson sí se comparten.
     */
    @Provides
    @Singleton
    fun providePlacesApiService(client: OkHttpClient, gson: Gson): PlacesApiService =
        Retrofit.Builder()
            .baseUrl(PLACES_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(PlacesApiService::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindingsModule {

    // Las fuentes de datos (Firestore o backend propio) las elige
    // DataSourceModule según ACTIVE_BACKEND; aquí solo van los repositorios.

    @Binds
    @Singleton
    abstract fun bindVehicleRepository(
        repository: VehicleRepositoryImpl
    ): VehicleRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        repository: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindReviewRepository(
        repository: ReviewRepositoryImpl
    ): ReviewRepository
}
