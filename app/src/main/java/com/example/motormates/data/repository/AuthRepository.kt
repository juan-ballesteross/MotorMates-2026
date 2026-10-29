package com.example.motormates.data.repository

import com.example.motormates.data.dto.UserDto
import com.example.motormates.data.model.AuthUser
import com.example.motormates.data.source.AuthDataSource
import com.example.motormates.data.source.UserDataSource
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException

interface AuthRepository {
    val authState: Flow<AuthUser?>

    suspend fun signIn(email: String, password: String): Result<AuthUser>

    suspend fun signUp(fullName: String, email: String, password: String): Result<AuthUser>

    suspend fun signOut(): Result<Unit>

    fun getCurrentUser(): AuthUser?

    suspend fun updateDisplayName(name: String): Result<Unit>
}

class AuthRepositoryImpl @Inject constructor(
    private val authDataSource: AuthDataSource,
    private val userDataSource: UserDataSource
) : AuthRepository {

    override val authState: Flow<AuthUser?> = authDataSource.authState

    override suspend fun signIn(email: String, password: String): Result<AuthUser> {
        return try {
            val user = authDataSource.signIn(email.trim(), password)
            Result.success(user)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Correo o contraseña incorrectos"))
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(Exception("No existe una cuenta con ese correo"))
        } catch (e: FirebaseNetworkException) {
            Result.failure(Exception("Sin conexión a internet, verifica tu red"))
        } catch (e: Exception) {
            Result.failure(Exception("Error al iniciar sesión"))
        }
    }

    override suspend fun signUp(fullName: String, email: String, password: String): Result<AuthUser> {
        return try {
            val cleanName = fullName.trim()
            val cleanEmail = email.trim()

            // Paso 1: crear la cuenta en Auth. Paso 2: guardar su perfil en Firestore.
            val user = authDataSource.signUp(cleanName, cleanEmail, password)
            saveUserProfile(user, cleanName, cleanEmail)

            Result.success(user)
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("Ya existe una cuenta registrada con ese correo"))
        } catch (e: FirebaseAuthWeakPasswordException) {
            Result.failure(Exception("La contraseña es muy débil, usa al menos 6 caracteres"))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("El correo no es válido"))
        } catch (e: FirebaseNetworkException) {
            Result.failure(Exception("Sin conexión a internet, verifica tu red"))
        } catch (e: FirebaseFirestoreException) {
            val message = when (e.code) {
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "Sin conexión a internet, verifica tu red"
                else -> "No se pudo guardar tu perfil, intenta de nuevo"
            }
            Result.failure(Exception(message))
        } catch (e: IOException) {
            // El perfil se guarda en el backend propio y no respondió.
            Result.failure(Exception(NO_CONNECTION_MESSAGE))
        } catch (e: HttpException) {
            Result.failure(Exception("No se pudo guardar tu perfil, intenta de nuevo"))
        } catch (e: Exception) {
            Result.failure(Exception("Error al registrar la cuenta"))
        }
    }

    override suspend fun signOut(): Result<Unit> =
        runCatching { authDataSource.signOut() }

    override fun getCurrentUser(): AuthUser? = authDataSource.getCurrentUser()

    override suspend fun updateDisplayName(name: String): Result<Unit> =
        runCatching { authDataSource.updateDisplayName(name.trim()) }

    /**
     * Si guardar el perfil falla, la cuenta de Auth ya existe sin perfil.
     * La borramos para que el usuario pueda reintentar el registro con el
     * mismo correo, y relanzamos el error para que el catch de signUp
     * lo traduzca al mensaje que corresponda.
     */
    private suspend fun saveUserProfile(user: AuthUser, fullName: String, email: String) {
        try {
            userDataSource.createUser(
                UserDto(
                    id = user.id,
                    fullName = fullName,
                    email = email,
                    // Valor inicial: lo que va antes de la @ del correo. Se podrá editar.
                    username = email.substringBefore("@"),
                    createdAt = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            runCatching { authDataSource.deleteCurrentUser() }
            throw e
        }
    }
}