package com.example.motormates.data.repository

import com.example.motormates.data.model.BackendUser
import com.example.motormates.data.remote.datasource.UserRemoteDataSource
import com.example.motormates.data.remote.mapper.toDomain
import java.io.IOException
import javax.inject.Inject
import retrofit2.HttpException

interface UserRepository {
    suspend fun getUserById(id: Int): Result<BackendUser>
}

class UserRepositoryImpl @Inject constructor(
    private val remoteDataSource: UserRemoteDataSource
) : UserRepository {

    override suspend fun getUserById(id: Int): Result<BackendUser> {
        return try {
            Result.success(remoteDataSource.getUserById(id).toDomain())
        } catch (e: IOException) {
            Result.failure(Exception(NO_CONNECTION_MESSAGE))
        } catch (e: HttpException) {
            Result.failure(
                Exception(
                    e.toSpanishMessage(
                        notFound = "Este usuario no existe",
                        fallback = "No se pudo cargar el perfil"
                    )
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo cargar el perfil"))
        }
    }
}
