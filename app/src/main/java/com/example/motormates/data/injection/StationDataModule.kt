package com.example.motormates.data.injection

import com.example.motormates.data.repository.StationRepository
import com.example.motormates.data.repository.StationRepositoryImpl
import com.example.motormates.data.source.PlacesStationDataSource
import com.example.motormates.data.source.StationDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Las estaciones no pasan por el `pick(firebase, express)` de DataSourceModule.
 *
 * Ese helper existe para las fuentes que tienen dos implementaciones y se
 * intercambian según ACTIVE_BACKEND. Las estaciones solo salen de Google
 * Places: ni Firestore ni el backend propio tienen esos datos, así que una
 * segunda implementación sería un archivo muerto. Se enlaza directo, igual que
 * EventDataModule y LocationDataModule, que documentan el mismo motivo.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class StationDataModule {

    @Binds
    @Singleton
    abstract fun bindStationDataSource(impl: PlacesStationDataSource): StationDataSource

    @Binds
    @Singleton
    abstract fun bindStationRepository(impl: StationRepositoryImpl): StationRepository
}
