package com.example.motormates.data.injection

import android.content.Context
import android.location.Geocoder
import com.example.motormates.data.source.DeviceLocationDataSource
import com.example.motormates.data.source.FusedDeviceLocationDataSource
import com.example.motormates.data.source.GeocoderPlaceNameDataSource
import com.example.motormates.data.source.PlaceNameDataSource
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.Locale
import javax.inject.Singleton

/**
 * La ubicación del dispositivo. A diferencia del resto de las fuentes de
 * datos, esta no se elige según ACTIVE_BACKEND: no viene de un servidor sino
 * del teléfono, así que no entra en el pick() de DataSourceModule.
 */
@Module
@InstallIn(SingletonComponent::class)
object LocationProvidersModule {

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    /**
     * Geocoder en español: los nombres de ciudad y región salen en el idioma
     * que se le pase, y la app está en español.
     */
    @Provides
    @Singleton
    fun provideGeocoder(@ApplicationContext context: Context): Geocoder =
        Geocoder(context, Locale("es", "CO"))
}

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationDataModule {

    @Binds
    @Singleton
    abstract fun bindDeviceLocationDataSource(
        impl: FusedDeviceLocationDataSource
    ): DeviceLocationDataSource

    @Binds
    @Singleton
    abstract fun bindPlaceNameDataSource(
        impl: GeocoderPlaceNameDataSource
    ): PlaceNameDataSource
}
