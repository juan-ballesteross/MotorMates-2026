package com.example.motormates.data.injection

import com.example.motormates.data.source.FirestoreReviewRealtimeDataSource
import com.example.motormates.data.source.ReviewRealtimeDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReviewRealtimeModule {

    @Binds
    @Singleton
    abstract fun bindReviewRealtimeDataSource(
        impl: FirestoreReviewRealtimeDataSource
    ): ReviewRealtimeDataSource
}
