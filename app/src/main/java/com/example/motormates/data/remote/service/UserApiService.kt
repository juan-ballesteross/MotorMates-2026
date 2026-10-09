package com.example.motormates.data.remote.service

import com.example.motormates.data.remote.dto.UserDto
import retrofit2.http.GET
import retrofit2.http.Path

interface UserApiService {

    @GET("users/{id}")
    suspend fun getUserById(@Path("id") id: Int): UserDto
}
