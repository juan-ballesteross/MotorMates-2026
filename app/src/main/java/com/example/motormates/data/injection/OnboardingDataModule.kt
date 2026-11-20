package com.example.motormates.data.injection

import com.example.motormates.data.repository.PreferencesRepository
import com.example.motormates.data.repository.PreferencesRepositoryImpl
import com.example.motormates.data.source.FirestorePreferencesDataSource
import com.example.motormates.data.source.PreferencesDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Onboarding y personalización. Se enlaza directo y no por el
 * pick(firebase, express) de DataSourceModule: las preferencias solo viven
 * en Firestore, igual que StationDataModule documenta para Places.
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
}
