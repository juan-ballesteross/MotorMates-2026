package com.example.motormates.data.injection

import com.example.motormates.data.source.FirestoreUserDataSource
import com.example.motormates.data.source.UserDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UserDataModule {

    @Binds
    @Singleton
    abstract fun bindUserDataSource(impl: FirestoreUserDataSource): UserDataSource
}