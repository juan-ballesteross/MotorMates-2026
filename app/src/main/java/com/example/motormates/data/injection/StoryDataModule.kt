package com.example.motormates.data.injection

import com.example.motormates.data.repository.StoryRepository
import com.example.motormates.data.repository.StoryRepositoryImpl
import com.example.motormates.data.source.FirestoreStoryDataSource
import com.example.motormates.data.source.StoryDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StoryDataModule {

    @Binds
    @Singleton
    abstract fun bindStoryDataSource(impl: FirestoreStoryDataSource): StoryDataSource

    @Binds
    @Singleton
    abstract fun bindStoryRepository(impl: StoryRepositoryImpl): StoryRepository
}
