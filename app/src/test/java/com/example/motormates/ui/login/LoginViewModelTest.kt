package com.example.motormates.ui.login

import android.content.Context
import com.example.motormates.MainDispatcherRule
import com.example.motormates.data.model.AuthUser
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.UserRepository
import com.example.motormates.data.source.GoogleSignInDataSource
import com.example.motormates.data.source.GoogleSignInException
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * El inicio de sesión con Google en el ViewModel.
 *
 * El Context es un mockk que se pasa de largo: el ViewModel no lo toca, solo
 * lo reenvía al GoogleSignInDataSource, que acá también está mockeado. Por eso
 * esto corre en la JVM sin Credential Manager ni Play Services.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val activityContext: Context = mockk()
    private val authRepository: AuthRepository = mockk()
    private val userRepository: UserRepository = mockk()
    private val googleSignIn: GoogleSignInDataSource = mockk()

    private fun viewModel() = LoginViewModel(authRepository, userRepository, googleSignIn)

    @Test
    fun `cerrar el selector no deja error ni loading colgado`() = runTest {
        // null = el usuario cerró el selector sin elegir cuenta.
        coEvery { googleSignIn.idToken(activityContext) } returns null

        val vm = viewModel()
        vm.signInWithGoogle(activityContext)
        advanceUntilIdle()

        assertThat(vm.uiState.value.isLoading).isFalse()
        assertThat(vm.uiState.value.errorMessage).isNull()
        assertThat(vm.uiState.value.isLoggedIn).isFalse()
        // Cancelar no debe llegar a Firebase.
        coVerify(exactly = 0) { authRepository.signInWithGoogle(any()) }
    }

    @Test
    fun `sin cuentas de google en el dispositivo avisa`() = runTest {
        coEvery { googleSignIn.idToken(activityContext) } throws
            GoogleSignInException(GoogleSignInException.Reason.NO_ACCOUNTS)

        val vm = viewModel()
        vm.signInWithGoogle(activityContext)
        advanceUntilIdle()

        assertThat(vm.uiState.value.isLoading).isFalse()
        assertThat(vm.uiState.value.errorMessage)
            .isEqualTo("No hay ninguna cuenta de Google en este dispositivo")
    }

    @Test
    fun `un error de configuracion no se le muestra crudo al usuario`() = runTest {
        coEvery { googleSignIn.idToken(activityContext) } throws GoogleSignInException(
            GoogleSignInException.Reason.MISCONFIGURED,
            IllegalStateException("SHA-1 no registrado en Firebase")
        )

        val vm = viewModel()
        vm.signInWithGoogle(activityContext)
        advanceUntilIdle()

        val message = vm.uiState.value.errorMessage
        assertThat(message).isNotNull()
        // El detalle técnico queda en la excepción, no en la pantalla.
        assertThat(message).doesNotContain("SHA-1")
    }

    @Test
    fun `con una cuenta elegida entra`() = runTest {
        val user = AuthUser(id = "uid-g", email = "ana@gmail.com", displayName = "Ana")
        coEvery { googleSignIn.idToken(activityContext) } returns "id-token"
        coEvery { authRepository.signInWithGoogle("id-token") } returns Result.success(user)

        val vm = viewModel()
        vm.signInWithGoogle(activityContext)
        advanceUntilIdle()

        assertThat(vm.uiState.value.isLoggedIn).isTrue()
        assertThat(vm.uiState.value.isLoading).isFalse()
        assertThat(vm.uiState.value.errorMessage).isNull()
    }

    @Test
    fun `si firebase falla se muestra el mensaje del repositorio`() = runTest {
        coEvery { googleSignIn.idToken(activityContext) } returns "id-token"
        coEvery { authRepository.signInWithGoogle("id-token") } returns
            Result.failure(Exception("Sin conexión a internet, verifica tu red"))

        val vm = viewModel()
        vm.signInWithGoogle(activityContext)
        advanceUntilIdle()

        assertThat(vm.uiState.value.isLoggedIn).isFalse()
        assertThat(vm.uiState.value.isLoading).isFalse()
        assertThat(vm.uiState.value.errorMessage)
            .isEqualTo("Sin conexión a internet, verifica tu red")
    }

    @Test
    fun `no abre el selector dos veces si ya esta cargando`() = runTest {
        coEvery { googleSignIn.idToken(activityContext) } returns "id-token"
        coEvery { authRepository.signInWithGoogle("id-token") } returns
            Result.success(AuthUser(id = "uid-g", email = null, displayName = null))

        val vm = viewModel()
        // El segundo toque cae mientras el primero sigue en vuelo: el
        // dispatcher de prueba no avanzó, así que isLoading ya está en true.
        vm.signInWithGoogle(activityContext)
        vm.signInWithGoogle(activityContext)
        advanceUntilIdle()

        coVerify(exactly = 1) { googleSignIn.idToken(activityContext) }
    }
}
