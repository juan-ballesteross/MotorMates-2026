package com.example.motormates.ui.splash

import androidx.lifecycle.ViewModel
import com.example.motormates.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    fun isUserLoggedIn(): Boolean = authRepository.getCurrentUser() != null
}
