package com.example.motormates.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.PushTokenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * Registra el token de FCM de este dispositivo cada vez que hay sesión.
 *
 * No tiene estado ni pantalla: se crea una sola vez en la raíz
 * (MotorMatesApp) y vive mientras viva la app. Se cuelga de authState, que ya
 * es el AuthStateListener de Firebase, para cubrir los tres casos con un solo
 * punto: iniciar sesión, registrarse y abrir la app con la sesión guardada.
 * El borrado al cerrar sesión no está acá, lo hace AuthRepositoryImpl.signOut.
 */
@HiltViewModel
class PushTokenViewModel @Inject constructor(
    authRepository: AuthRepository,
    pushTokenRepository: PushTokenRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            authRepository.authState.collect { user ->
                if (user != null) pushTokenRepository.registerCurrentDevice()
            }
        }
    }
}
