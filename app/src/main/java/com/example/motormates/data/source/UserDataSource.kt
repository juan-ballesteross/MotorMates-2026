package com.example.motormates.data.source

import com.example.motormates.data.dto.UserDto

interface UserDataSource {
    suspend fun createUser(user: UserDto)
}