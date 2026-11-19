package com.example.motormates.data.injection

import com.example.motormates.data.repository.EventRepository
import com.example.motormates.data.repository.EventRepositoryImpl
import com.example.motormates.data.source.EventDataSource
import com.example.motormates.data.source.EventParticipationDataSource
import com.example.motormates.data.source.FirestoreEventDataSource
import com.example.motormates.data.source.FirestoreEventParticipationDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Los eventos no pasan por el `pick(firebase, express)` de DataSourceModule.
 *
 * Ese helper existe para las fuentes que tienen dos implementaciones y se
 * intercambian según ACTIVE_BACKEND. Los eventos solo viven en Firestore: el
 * backend propio no tiene esas tablas, así que una implementación de Retrofit
 * sería un archivo muerto. Se enlaza directo, igual que PushTokenDataModule y
 * LocationDataModule, que documentan el mismo motivo.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class EventDataModule {

    @Binds
    @Singleton
    abstract fun bindEventDataSource(impl: FirestoreEventDataSource): EventDataSource

    @Binds
    @Singleton
    abstract fun bindEventParticipationDataSource(
        impl: FirestoreEventParticipationDataSource
    ): EventParticipationDataSource

    @Binds
    @Singleton
    abstract fun bindEventRepository(impl: EventRepositoryImpl): EventRepository
}
