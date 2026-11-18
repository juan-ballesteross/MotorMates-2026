package com.example.motormates.ui.editProfile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.garageOf
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.CoverImageRepository
import com.example.motormates.data.repository.ProfileImageRepository
import com.example.motormates.data.repository.UserRepository
import com.example.motormates.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileImageRepository: ProfileImageRepository,
    private val coverImageRepository: CoverImageRepository,
    private val userRepository: UserRepository,
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState

    init {
        viewModelScope.launch {
            profileImageRepository.profileImageUrl.collect { photoUrl ->
                _uiState.update { it.copy(profileImageUrl = photoUrl) }
            }
        }

        // El garaje no se carga aquí: lo pide la pantalla en ON_RESUME, que
        // también corre al abrirla. Así no se consulta dos veces al entrar.
        loadProfile()
    }

    /**
     * Mis vehículos publicados. La pantalla la vuelve a llamar al regresar de
     * editar o publicar uno, para que la lista no quede vieja. Si falla se
     * deja lo que ya había.
     */
    fun loadGarage() {
        val uid = authRepository.getCurrentUser()?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isGarageLoading = it.cars.isEmpty()) }
            vehicleRepository.getVehicles().onSuccess { vehicles ->
                _uiState.update { it.copy(cars = vehicles.garageOf(uid)) }
            }
            _uiState.update { it.copy(isGarageLoading = false) }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val authUser = authRepository.getCurrentUser() ?: return@launch
            userRepository.getCurrentUserProfile(authUser.id).onSuccess { user ->
                _uiState.update {
                    it.copy(
                        fullName = user.fullName,
                        username = user.username,
                        location = user.location,
                        bio = user.bio,
                        coverImageUrl = user.coverUrl
                    )
                }
            }
        }
    }

    fun updateFullName(input: String) {
        _uiState.update { it.copy(fullName = input) }
    }

    fun updateUsername(input: String) {
        _uiState.update { it.copy(username = input) }
    }

    fun updateLocation(input: String) {
        _uiState.update { it.copy(location = input) }
    }

    fun updateBio(input: String) {
        _uiState.update { it.copy(bio = input) }
    }

    fun saveProfile(onSaved: () -> Unit) {
        val authUser = authRepository.getCurrentUser()
        if (authUser == null) {
            onSaved()
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }

            val form = _uiState.value
            val result = userRepository.updateCurrentUserProfile(
                uid = authUser.id,
                fullName = form.fullName,
                username = form.username,
                location = form.location,
                bio = form.bio
            )

            result.fold(
                onSuccess = {
                    // Best-effort, como la foto: que falle no deshace lo guardado.
                    authRepository.updateDisplayName(form.fullName)
                    _uiState.update { it.copy(isSaving = false) }
                    onSaved()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveError = error.message ?: "No se pudo guardar tu perfil"
                        )
                    }
                }
            )
        }
    }

    fun logOut(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onLoggedOut()
        }
    }

    fun uploadProfileImage(uri: Uri) {
        if (_uiState.value.isImageUploading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isImageUploading = true, imageUploadError = null) }
            val result = profileImageRepository.uploadProfileImage(uri)
            _uiState.update {
                it.copy(
                    isImageUploading = false,
                    imageUploadError = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun uploadCoverImage(uri: Uri) {
        if (_uiState.value.isCoverUploading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCoverUploading = true, coverUploadError = null) }
            val result = coverImageRepository.uploadCoverImage(uri)
            _uiState.update {
                it.copy(
                    isCoverUploading = false,
                    coverImageUrl = result.getOrNull() ?: it.coverImageUrl,
                    coverUploadError = result.exceptionOrNull()?.message
                )
            }
        }
    }
}
