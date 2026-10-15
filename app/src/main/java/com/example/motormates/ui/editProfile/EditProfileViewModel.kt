package com.example.motormates.ui.editProfile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.mock.UserMocks
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.CoverImageRepository
import com.example.motormates.data.repository.ProfileImageRepository
import com.example.motormates.data.repository.UserRepository
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
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState

    init {
        // El garaje se queda con datos locales: el backend no lo modela.
        _uiState.update { it.copy(cars = UserMocks.sampleUserCars) }

        viewModelScope.launch {
            profileImageRepository.profileImageUrl.collect { photoUrl ->
                _uiState.update { it.copy(profileImageUrl = photoUrl) }
            }
        }

        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val authUser = authRepository.getCurrentUser() ?: return@launch
            userRepository.getCurrentUserProfile(authUser.id).onSuccess { user ->
                _uiState.update {
                    it.copy(username = user.fullName, bio = user.bio, coverImageUrl = user.coverUrl)
                }
            }
        }
    }

    fun updateUsername(input: String) {
        _uiState.update { it.copy(username = input) }
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

            val result = userRepository.updateCurrentUserProfile(
                uid = authUser.id,
                fullName = _uiState.value.username,
                bio = _uiState.value.bio
            )

            result.fold(
                onSuccess = {
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
