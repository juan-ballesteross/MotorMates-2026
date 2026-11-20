package com.example.motormates.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.UserRepository
import com.example.motormates.data.source.GoogleSignInDataSource
import com.example.motormates.data.source.GoogleSignInException
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    // El selector de cuentas de Google es una capacidad del teléfono, no un
    // backend, así que entra directo como DataSource igual que
    // DeviceLocationDataSource en ReviewsMapViewModel.
    private val googleSignInDataSource: GoogleSignInDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun updateEmail(input: String) {
        _uiState.update { it.copy(email = input, errorMessage = null) }
    }

    fun updatePassword(input: String) {
        _uiState.update { it.copy(password = input, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun signIn() {
        val currentState = _uiState.value
        if (currentState.isLoading || currentState.email.isBlank() || currentState.password.isBlank()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    isLoggedIn = false
                )
            }

            val result = authRepository.signIn(currentState.email, currentState.password)

            // La cuenta puede autenticarse y no tener perfil en esta base de
            // datos (cuentas de versiones anteriores de la app). Si se entrara
            // igual, el Splash cerraría la sesión en el siguiente arranque y
            // el usuario quedaría dando vueltas.
            // getOrDefault(true): si no se pudo comprobar (sin red) se deja
            // pasar, mismo criterio que el Splash.
            val user = result.getOrNull()
            if (user != null && !userRepository.hasProfile(user.id).getOrDefault(true)) {
                authRepository.signOut()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = ACCOUNT_WITHOUT_PROFILE,
                        isLoggedIn = false
                    )
                }
                return@launch
            }

            _uiState.update {
                if (result.isSuccess) {
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                        isLoggedIn = true
                    )
                } else {
                    it.copy(
                        isLoading = false,
                        // Antes: mensaje fijo que ignoraba el error real.
                        // Ahora: usa el mensaje específico que armó el
                        // Repository (credenciales, red, etc.), con un
                        // genérico de respaldo solo por si acaso.
                        errorMessage = result.exceptionOrNull()?.message
                            ?: "Error al iniciar sesión",
                        isLoggedIn = false
                    )
                }
            }
        }
    }

    /**
     * Inicio de sesión con Google, de punta a punta: abre el selector de
     * cuentas, y con el idToken que devuelva autentica en Firebase.
     *
     * @param activityContext contexto de la **Activity**, que el selector
     *   necesita para montarse (el de aplicación no sirve). Llega por
     *   parámetro y se pasa de largo al DataSource: no se guarda en el
     *   ViewModel, que vive más que la Activity.
     */
    fun signInWithGoogle(activityContext: Context) {
        if (_uiState.value.isLoading) return

        // El loading se marca aquí y no dentro del launch, por dos razones: el
        // botón tiene que dar señal de vida mientras Google se toma su tiempo,
        // y un segundo toque rápido tiene que ver el estado ya cambiado para
        // que la guarda de arriba lo corte. Si se marcara dentro del launch,
        // el cuerpo corre después y los dos toques abrirían el selector.
        _uiState.update { it.copy(isLoading = true, errorMessage = null, isLoggedIn = false) }

        viewModelScope.launch {
            val idToken = try {
                googleSignInDataSource.idToken(activityContext)
            } catch (e: GoogleSignInException) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.reason.toMessage(), isLoggedIn = false)
                }
                return@launch
            }

            // null = cerró el selector sin elegir. No es un error: se apaga el
            // loading y la pantalla queda como estaba, sin mensaje rojo.
            if (idToken == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                return@launch
            }

            val result = authRepository.signInWithGoogle(idToken)

            _uiState.update {
                if (result.isSuccess) {
                    it.copy(isLoading = false, errorMessage = null, isLoggedIn = true)
                } else {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message
                            ?: "Error al iniciar sesión con Google",
                        isLoggedIn = false
                    )
                }
            }
        }
    }

    /**
     * El motivo, en palabras para el usuario. Vive aquí y no en el
     * repositorio porque el repositorio nunca ve esta excepción: recibe un
     * idToken ya obtenido. Mismo criterio que ACCOUNT_WITHOUT_PROFILE.
     */
    private fun GoogleSignInException.Reason.toMessage(): String = when (this) {
        GoogleSignInException.Reason.NO_ACCOUNTS ->
            "No hay ninguna cuenta de Google en este dispositivo"
        GoogleSignInException.Reason.MISCONFIGURED ->
            "No se pudo iniciar sesión con Google. Avísale al equipo: la app no está bien configurada."
        GoogleSignInException.Reason.NETWORK ->
            "Sin conexión a internet, verifica tu red"
        GoogleSignInException.Reason.UNKNOWN ->
            "Error al iniciar sesión con Google"
    }

    private companion object {
        const val ACCOUNT_WITHOUT_PROFILE =
            "Esta cuenta es de una versión anterior de la app y ya no tiene perfil. " +
                "Regístrate con otro correo."
    }
}
