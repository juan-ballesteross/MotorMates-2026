package com.example.motormates.data.remote.datasource

import com.example.motormates.data.remote.dto.UserDto

interface UserRemoteDataSource {

    suspend fun getUserById(id: Int): UserDto
}
