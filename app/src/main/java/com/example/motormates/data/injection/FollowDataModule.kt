package com.example.motormates.data.injection

import com.example.motormates.data.repository.FollowRepository
import com.example.motormates.data.repository.FollowRepositoryImpl
import com.example.motormates.data.source.FirestoreFollowDataSource
import com.example.motormates.data.source.FollowDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FollowDataModule {

    @Binds
    @Singleton
    abstract fun bindFollowDataSource(impl: FirestoreFollowDataSource): FollowDataSource

    @Binds
    @Singleton
    abstract fun bindFollowRepository(impl: FollowRepositoryImpl): FollowRepository
}
