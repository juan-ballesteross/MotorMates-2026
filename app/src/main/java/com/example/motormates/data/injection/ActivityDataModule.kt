package com.example.motormates.data.injection

import com.example.motormates.data.repository.ActivityRepository
import com.example.motormates.data.repository.ActivityRepositoryImpl
import com.example.motormates.data.source.ActivityDataSource
import com.example.motormates.data.source.EmptyActivityDataSource
import com.example.motormates.data.source.FirestoreActivityDataSource
import com.example.motormates.di.ACTIVE_BACKEND
import com.example.motormates.di.DataBackend
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

/** La pestaña Actividad. Con el backend propio no hay likes, seguidos ni historias. */
@Module
@InstallIn(SingletonComponent::class)
object ActivityDataSourceModule {

    @Provides
    @Singleton
    fun provideActivityDataSource(
        firebase: Provider<FirestoreActivityDataSource>,
        express: Provider<EmptyActivityDataSource>
    ): ActivityDataSource = when (ACTIVE_BACKEND) {
        DataBackend.FIREBASE -> firebase.get()
        DataBackend.EXPRESS -> express.get()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ActivityDataModule {

    @Binds
    @Singleton
    abstract fun bindActivityRepository(impl: ActivityRepositoryImpl): ActivityRepository
}
