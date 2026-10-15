package com.example.motormates.di

import com.example.motormates.data.repository.CoverImageRepository
import com.example.motormates.data.repository.CoverImageRepositoryImpl
import com.example.motormates.data.repository.ProfileImageRepository
import com.example.motormates.data.repository.ProfileImageRepositoryImpl
import com.example.motormates.data.source.CoverImageDataSource
import com.example.motormates.data.source.FirebaseCoverImageDataSource
import com.example.motormates.data.source.FirebaseProfileImageDataSource
import com.example.motormates.data.source.ProfileImageDataSource
import com.google.firebase.storage.FirebaseStorage
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileImageBindingsModule {

    @Binds
    @Singleton
    abstract fun bindProfileImageDataSource(
        dataSource: FirebaseProfileImageDataSource
    ): ProfileImageDataSource

    @Binds
    @Singleton
    abstract fun bindProfileImageRepository(
        repository: ProfileImageRepositoryImpl
    ): ProfileImageRepository

    @Binds
    @Singleton
    abstract fun bindCoverImageDataSource(
        dataSource: FirebaseCoverImageDataSource
    ): CoverImageDataSource

    @Binds
    @Singleton
    abstract fun bindCoverImageRepository(
        repository: CoverImageRepositoryImpl
    ): CoverImageRepository
}

@Module
@InstallIn(SingletonComponent::class)
object ProfileImageFirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()
}
