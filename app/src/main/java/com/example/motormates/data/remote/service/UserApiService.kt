package com.example.motormates.data.remote.service

import com.example.motormates.data.dto.NotificationDto
import com.example.motormates.data.dto.UserDto
import com.example.motormates.data.remote.dto.CreateNotificationRequest
import com.example.motormates.data.remote.dto.CreateUserRequest
import com.example.motormates.data.remote.dto.FollowRequest
import com.example.motormates.data.remote.dto.UpdateUserRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Todo lo que cuelga de /users. El backend responde con la misma forma que
 * los documentos de Firestore (UserDto, NotificationDto), así que se reusan
 * esos DTOs y el resto de la app no distingue de dónde vino el dato.
 */
interface UserApiService {

    @POST("users")
    suspend fun createUser(@Body body: CreateUserRequest): UserDto

    /** Lanza HttpException 404 si el usuario no existe. */
    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: String): UserDto

    /** Sin username trae todos (buscador); con username, solo quien lo tenga. */
    @GET("users")
    suspend fun getUsers(@Query("username") username: String? = null): List<UserDto>

    @PATCH("users/{id}")
    suspend fun updateUser(@Path("id") id: String, @Body body: UpdateUserRequest): UserDto

    @POST("users/{id}/followers")
    suspend fun follow(@Path("id") targetId: String, @Body body: FollowRequest)

    @DELETE("users/{id}/followers/{followerId}")
    suspend fun unfollow(@Path("id") targetId: String, @Path("followerId") followerId: String)

    /** 200 si lo sigue, HttpException 404 si no. */
    @GET("users/{id}/followers/{followerId}")
    suspend fun checkFollowing(@Path("id") targetId: String, @Path("followerId") followerId: String)

    @GET("users/{id}/followers")
    suspend fun getFollowers(@Path("id") id: String): List<UserDto>

    @GET("users/{id}/following")
    suspend fun getFollowing(@Path("id") id: String): List<UserDto>

    /** reviewIds separados por coma; devuelve los que el usuario ya likeó. */
    @GET("users/{id}/likes")
    suspend fun getLikedReviewIds(
        @Path("id") userId: String,
        @Query("reviewIds") reviewIds: String
    ): List<String>

    @POST("users/{id}/notifications")
    suspend fun createNotification(
        @Path("id") recipientId: String,
        @Body body: CreateNotificationRequest
    )

    @GET("users/{id}/notifications")
    suspend fun getNotifications(@Path("id") id: String): List<NotificationDto>
}
