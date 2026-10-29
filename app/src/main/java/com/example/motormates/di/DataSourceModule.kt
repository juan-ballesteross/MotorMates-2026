package com.example.motormates.di

import com.example.motormates.data.remote.datasource.FollowRetrofitDataSource
import com.example.motormates.data.remote.datasource.LikeRetrofitDataSource
import com.example.motormates.data.remote.datasource.NotificationRetrofitDataSource
import com.example.motormates.data.remote.datasource.ReviewRealtimeRetrofitDataSource
import com.example.motormates.data.remote.datasource.ReviewRemoteDataSource
import com.example.motormates.data.remote.datasource.ReviewRetrofitDataSource
import com.example.motormates.data.remote.datasource.UserRetrofitDataSource
import com.example.motormates.data.remote.datasource.VehicleRemoteDataSource
import com.example.motormates.data.remote.datasource.VehicleRetrofitDataSource
import com.example.motormates.data.source.FirestoreFollowDataSource
import com.example.motormates.data.source.FirestoreLikeDataSource
import com.example.motormates.data.source.FirestoreNotificationDataSource
import com.example.motormates.data.source.FirestoreReviewDataSource
import com.example.motormates.data.source.FirestoreReviewRealtimeDataSource
import com.example.motormates.data.source.FirestoreUserDataSource
import com.example.motormates.data.source.FirestoreVehicleDataSource
import com.example.motormates.data.source.FollowDataSource
import com.example.motormates.data.source.LikeDataSource
import com.example.motormates.data.source.NotificationDataSource
import com.example.motormates.data.source.ReviewRealtimeDataSource
import com.example.motormates.data.source.UserDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Cada fuente de datos tiene dos implementaciones de la misma interfaz:
 * una contra Firestore y otra contra el backend propio (Retrofit). Aquí se
 * escoge cuál inyectar según ACTIVE_BACKEND (DataBackend.kt).
 *
 * Se reciben como Provider para que Hilt solo construya la implementación
 * elegida: la otra nunca se instancia.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {

    private fun <T> pick(firebase: Provider<out T>, express: Provider<out T>): T =
        when (ACTIVE_BACKEND) {
            DataBackend.FIREBASE -> firebase.get()
            DataBackend.EXPRESS -> express.get()
        }

    @Provides
    @Singleton
    fun provideVehicleDataSource(
        firebase: Provider<FirestoreVehicleDataSource>,
        express: Provider<VehicleRetrofitDataSource>
    ): VehicleRemoteDataSource = pick(firebase, express)

    @Provides
    @Singleton
    fun provideReviewDataSource(
        firebase: Provider<FirestoreReviewDataSource>,
        express: Provider<ReviewRetrofitDataSource>
    ): ReviewRemoteDataSource = pick(firebase, express)

    @Provides
    @Singleton
    fun provideReviewRealtimeDataSource(
        firebase: Provider<FirestoreReviewRealtimeDataSource>,
        express: Provider<ReviewRealtimeRetrofitDataSource>
    ): ReviewRealtimeDataSource = pick(firebase, express)

    @Provides
    @Singleton
    fun provideUserDataSource(
        firebase: Provider<FirestoreUserDataSource>,
        express: Provider<UserRetrofitDataSource>
    ): UserDataSource = pick(firebase, express)

    @Provides
    @Singleton
    fun provideLikeDataSource(
        firebase: Provider<FirestoreLikeDataSource>,
        express: Provider<LikeRetrofitDataSource>
    ): LikeDataSource = pick(firebase, express)

    @Provides
    @Singleton
    fun provideFollowDataSource(
        firebase: Provider<FirestoreFollowDataSource>,
        express: Provider<FollowRetrofitDataSource>
    ): FollowDataSource = pick(firebase, express)

    @Provides
    @Singleton
    fun provideNotificationDataSource(
        firebase: Provider<FirestoreNotificationDataSource>,
        express: Provider<NotificationRetrofitDataSource>
    ): NotificationDataSource = pick(firebase, express)
}
