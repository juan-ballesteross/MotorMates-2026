package com.example.motormates.di

import com.example.motormates.data.remote.BASE_URL
import com.example.motormates.data.remote.ReviewApiService
import com.example.motormates.data.remote.ReviewRemoteDataSource
import com.example.motormates.data.remote.ReviewRetrofitDataSource
import com.example.motormates.data.remote.UserApiService
import com.example.motormates.data.remote.UserRemoteDataSource
import com.example.motormates.data.remote.UserRetrofitDataSource
import com.example.motormates.data.remote.VehicleApiService
import com.example.motormates.data.remote.VehicleRemoteDataSource
import com.example.motormates.data.remote.VehicleRetrofitDataSource
import com.example.motormates.data.repository.ReviewRepository
import com.example.motormates.data.repository.ReviewRepositoryImpl
import com.example.motormates.data.repository.UserRepository
import com.example.motormates.data.repository.UserRepositoryImpl
import com.example.motormates.data.repository.VehicleRepository
import com.example.motormates.data.repository.VehicleRepositoryImpl
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

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
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindingsModule {

    @Binds
    @Singleton
    abstract fun bindVehicleRemoteDataSource(
        dataSource: VehicleRetrofitDataSource
    ): VehicleRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindUserRemoteDataSource(
        dataSource: UserRetrofitDataSource
    ): UserRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindReviewRemoteDataSource(
        dataSource: ReviewRetrofitDataSource
    ): ReviewRemoteDataSource

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
