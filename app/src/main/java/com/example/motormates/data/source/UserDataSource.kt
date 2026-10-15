package com.example.motormates.data.source

import com.example.motormates.data.dto.UserDto

interface UserDataSource {
    suspend fun createUser(user: UserDto)
    suspend fun getUser(uid: String): UserDto?
    suspend fun updateUser(uid: String, fullName: String, bio: String)
    suspend fun updateAvatarUrl(uid: String, avatarUrl: String)
    suspend fun updateCoverUrl(uid: String, coverUrl: String)
    suspend fun getAllUsers(): List<UserDto>
}