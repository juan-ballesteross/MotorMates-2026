package com.example.motormates.data.injection

import com.example.motormates.data.repository.AiTuningRepository
import com.example.motormates.data.repository.AiTuningRepositoryImpl
import com.example.motormates.data.repository.BuildRepository
import com.example.motormates.data.repository.BuildRepositoryImpl
import com.example.motormates.data.repository.PartsCatalogRepository
import com.example.motormates.data.repository.PartsCatalogRepositoryImpl
import com.example.motormates.data.source.AiModelConfig
import com.example.motormates.data.source.AiUsageDataSource
import com.example.motormates.data.source.AndroidTuningImageSource
import com.example.motormates.data.source.AssetPartsCatalogDataSource
import com.example.motormates.data.source.BuildDataSource
import com.example.motormates.data.source.FirebaseAiTuningDataSource
import com.example.motormates.data.source.FirestoreAiUsageDataSource
import com.example.motormates.data.source.FirestoreBuildDataSource
import com.example.motormates.data.source.FirestorePartsCatalogDataSource
import com.example.motormates.data.source.GeminiTuningDataSource
import com.example.motormates.data.source.LocalPartsCatalogDataSource
import com.example.motormates.data.source.RemoteConfigAiModelConfig
import com.example.motormates.data.source.RemotePartsCatalogDataSource
import com.example.motormates.data.source.TuningImageSource
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.example.motormates.notifications.AiNotifier
import com.example.motormates.notifications.AndroidAiNotifier
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * AI Tuning solo existe con Firebase (Gemini vía Firebase AI Logic), así que
 * no pasa por el pick() de DataSourceModule: se enlaza directo, como Stories.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiTuningDataModule {

    /** Notificaciones de las tareas de AI en segundo plano. */
    @Binds
    @Singleton
    abstract fun bindAiNotifier(impl: AndroidAiNotifier): AiNotifier

    @Binds
    @Singleton
    abstract fun bindRemotePartsCatalog(impl: FirestorePartsCatalogDataSource): RemotePartsCatalogDataSource

    @Binds
    @Singleton
    abstract fun bindLocalPartsCatalog(impl: AssetPartsCatalogDataSource): LocalPartsCatalogDataSource

    @Binds
    @Singleton
    abstract fun bindGeminiTuningDataSource(impl: FirebaseAiTuningDataSource): GeminiTuningDataSource

    @Binds
    @Singleton
    abstract fun bindTuningImageSource(impl: AndroidTuningImageSource): TuningImageSource

    @Binds
    @Singleton
    abstract fun bindAiUsageDataSource(impl: FirestoreAiUsageDataSource): AiUsageDataSource

    @Binds
    @Singleton
    abstract fun bindBuildDataSource(impl: FirestoreBuildDataSource): BuildDataSource

    @Binds
    @Singleton
    abstract fun bindAiModelConfig(impl: RemoteConfigAiModelConfig): AiModelConfig

    @Binds
    @Singleton
    abstract fun bindPartsCatalogRepository(impl: PartsCatalogRepositoryImpl): PartsCatalogRepository

    @Binds
    @Singleton
    abstract fun bindAiTuningRepository(impl: AiTuningRepositoryImpl): AiTuningRepository

    @Binds
    @Singleton
    abstract fun bindBuildRepository(impl: BuildRepositoryImpl): BuildRepository

    companion object {
        @Provides
        @Singleton
        fun provideRemoteConfig(): FirebaseRemoteConfig = Firebase.remoteConfig
    }
}
