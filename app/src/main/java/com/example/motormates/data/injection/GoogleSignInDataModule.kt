package com.example.motormates.data.injection

import android.content.Context
import com.example.motormates.R
import com.example.motormates.data.source.CredentialManagerGoogleSignInDataSource
import com.example.motormates.data.source.GoogleSignInDataSource
import com.example.motormates.data.source.GoogleWebClientId
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * El selector de cuentas de Google. Como la ubicación del dispositivo, no se
 * elige según ACTIVE_BACKEND: no viene de un servidor sino del teléfono, así
 * que no entra en el pick() de DataSourceModule.
 */
@Module
@InstallIn(SingletonComponent::class)
object GoogleSignInProvidersModule {

    /**
     * El web client id sale de los recursos acá y no dentro del DataSource,
     * para que el DataSource no tenga que guardar ningún Context. Leer un
     * recurso sí se puede con el de aplicación; lo que necesita una Activity
     * es mostrar el selector, y ese contexto llega por parámetro.
     */
    @Provides
    @Singleton
    @GoogleWebClientId
    fun provideGoogleWebClientId(@ApplicationContext context: Context): String =
        context.getString(R.string.google_web_client_id)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class GoogleSignInDataModule {

    @Binds
    @Singleton
    abstract fun bindGoogleSignInDataSource(
        impl: CredentialManagerGoogleSignInDataSource
    ): GoogleSignInDataSource
}
