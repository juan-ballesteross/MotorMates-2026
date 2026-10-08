package com.example.motormates.data.remote

import com.example.motormates.data.remote.dto.UserDto
import javax.inject.Inject

interface UserRemoteDataSource {

    suspend fun getUserById(id: Int): UserDto
}

class UserRetrofitDataSource @Inject constructor(
    private val service: UserApiService
) : UserRemoteDataSource {

    override suspend fun getUserById(id: Int): UserDto = service.getUserById(id)
}
