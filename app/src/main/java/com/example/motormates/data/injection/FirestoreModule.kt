package com.example.motormates.data.injection

import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirestoreModule {

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore =
        // La base de datos de este proyecto se llama "motormatesdb" y no
        // "(default)", porque así se creó en la consola de Firebase.
        FirebaseFirestore.getInstance("motormatesdb")
}