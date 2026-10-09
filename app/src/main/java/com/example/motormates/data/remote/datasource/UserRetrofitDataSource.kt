package com.example.motormates.data.remote.datasource

import com.example.motormates.data.remote.dto.UserDto
import com.example.motormates.data.remote.service.UserApiService
import javax.inject.Inject

class UserRetrofitDataSource @Inject constructor(
    private val service: UserApiService
) : UserRemoteDataSource {

    override suspend fun getUserById(id: Int): UserDto = service.getUserById(id)
}
