package com.example.motormates.di

import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.AuthRepositoryImpl
import com.example.motormates.data.source.AuthDataSource
import com.example.motormates.data.source.FirebaseAuthDataSource
import com.google.firebase.auth.FirebaseAuth
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthBindingsModule {

    @Binds
    @Singleton
    abstract fun bindAuthDataSource(
        firebaseAuthDataSource: FirebaseAuthDataSource
    ): AuthDataSource

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AuthFirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
}
