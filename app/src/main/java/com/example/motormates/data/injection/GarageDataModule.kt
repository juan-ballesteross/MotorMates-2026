package com.example.motormates.data.injection

import com.example.motormates.data.repository.GarageRepository
import com.example.motormates.data.repository.GarageRepositoryImpl
import com.example.motormates.data.source.FirebaseVehicleImageDataSource
import com.example.motormates.data.source.VehicleImageDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Publicar vehículos. La fuente que escribe el documento se elige por
 * backend en DataSourceModule; la foto siempre va a Firebase Storage.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class GarageDataModule {

    @Binds
    @Singleton
    abstract fun bindVehicleImageDataSource(impl: FirebaseVehicleImageDataSource): VehicleImageDataSource

    @Binds
    @Singleton
    abstract fun bindGarageRepository(impl: GarageRepositoryImpl): GarageRepository
}
