package com.example.motormates.ui.splash

import androidx.lifecycle.ViewModel
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.withTimeoutOrNull

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    /**
     * No basta con que haya sesión en Firebase Auth: también tiene que
     * existir su perfil en Firestore. Una cuenta creada contra otra base de
     * datos (o desde la consola) deja una sesión válida sin perfil, y así la
     * app entraba al Feed para después fallar en Perfil sin salida.
     */
    suspend fun hasUsableSession(): Boolean {
        val uid = authRepository.getCurrentUser()?.id ?: return false

        // Con un tope: la comprobación es una consulta de red y el splash no
        // puede quedarse colgado si Firestore no responde.
        val check = withTimeoutOrNull(PROFILE_CHECK_TIMEOUT_MS) {
            userRepository.hasProfile(uid)
        } ?: return true

        return check.fold(
            onSuccess = { exists ->
                // Sesión de una cuenta cuyo perfil no está en esta base de
                // datos: se cierra para que el usuario entre con otra.
                if (!exists) authRepository.signOut()
                exists
            },
            // No se pudo comprobar (sin red): se deja entrar. El perfil
            // mostrará su propio error, que es mejor que sacar al usuario.
            onFailure = { true }
        )
    }

    private companion object {
        const val PROFILE_CHECK_TIMEOUT_MS = 5_000L
    }
}
