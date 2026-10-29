package com.example.motormates.data.injection

import com.example.motormates.data.repository.PushTokenRepository
import com.example.motormates.data.repository.PushTokenRepositoryImpl
import com.example.motormates.data.source.FirestorePushTokenDataSource
import com.example.motormates.data.source.PushTokenDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * A diferencia de las demás fuentes de datos, esta no se elige en
 * DataSourceModule: tiene una sola implementación a propósito (ver el KDoc de
 * FirestorePushTokenDataSource), así que se amarra aquí directamente.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PushTokenDataModule {

    @Binds
    @Singleton
    abstract fun bindPushTokenDataSource(impl: FirestorePushTokenDataSource): PushTokenDataSource

    @Binds
    @Singleton
    abstract fun bindPushTokenRepository(impl: PushTokenRepositoryImpl): PushTokenRepository
}
