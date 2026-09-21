package com.example.motormates.data.repository

import com.example.motormates.data.model.AuthUser
import com.example.motormates.data.source.AuthDataSource
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authState: Flow<AuthUser?>

    suspend fun signIn(email: String, password: String): Result<AuthUser>

    suspend fun signUp(fullName: String, email: String, password: String): Result<AuthUser>

    suspend fun signOut(): Result<Unit>

    fun getCurrentUser(): AuthUser?
}

class AuthRepositoryImpl @Inject constructor(
    private val authDataSource: AuthDataSource
) : AuthRepository {

    override val authState: Flow<AuthUser?> = authDataSource.authState

    /**
     * Catches específicos primero, genérico al final — mismo orden que
     * usó el profesor. Cada catch arma su propio mensaje en español en
     * vez de dejar pasar el texto crudo de Firebase.
     */
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
            val user = authDataSource.signUp(fullName.trim(), email.trim(), password)
            Result.success(user)
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("Ya existe una cuenta registrada con ese correo"))
        } catch (e: FirebaseAuthWeakPasswordException) {
            Result.failure(Exception("La contraseña es muy débil, usa al menos 6 caracteres"))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("El correo no es válido"))
        } catch (e: FirebaseNetworkException) {
            Result.failure(Exception("Sin conexión a internet, verifica tu red"))
        } catch (e: Exception) {
            Result.failure(Exception("Error al registrar la cuenta"))
        }
    }

    // signOut se deja con runCatching: es una operación simple donde no
    // necesitamos distinguir tipos de error con mensajes específicos.
    override suspend fun signOut(): Result<Unit> =
        runCatching { authDataSource.signOut() }

    override fun getCurrentUser(): AuthUser? = authDataSource.getCurrentUser()
}