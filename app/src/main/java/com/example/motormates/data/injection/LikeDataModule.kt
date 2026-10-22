package com.example.motormates.data.injection

import com.example.motormates.data.repository.LikeRepository
import com.example.motormates.data.repository.LikeRepositoryImpl
import com.example.motormates.data.source.FirestoreLikeDataSource
import com.example.motormates.data.source.LikeDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LikeDataModule {

    @Binds
    @Singleton
    abstract fun bindLikeDataSource(impl: FirestoreLikeDataSource): LikeDataSource

    @Binds
    @Singleton
    abstract fun bindLikeRepository(impl: LikeRepositoryImpl): LikeRepository
}