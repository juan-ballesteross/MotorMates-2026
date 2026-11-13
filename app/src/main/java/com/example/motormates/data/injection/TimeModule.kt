package com.example.motormates.data.injection

import com.example.motormates.data.util.SystemTimeSource
import com.example.motormates.data.util.TimeSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TimeModule {

    @Binds
    @Singleton
    abstract fun bindTimeSource(impl: SystemTimeSource): TimeSource
}
