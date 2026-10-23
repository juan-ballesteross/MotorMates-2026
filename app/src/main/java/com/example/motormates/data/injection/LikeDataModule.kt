package com.example.motormates.data.injection

import com.example.motormates.data.repository.LikeRepository
import com.example.motormates.data.repository.LikeRepositoryImpl
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
    abstract fun bindLikeRepository(impl: LikeRepositoryImpl): LikeRepository
}