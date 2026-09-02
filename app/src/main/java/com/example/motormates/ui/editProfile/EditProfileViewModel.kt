package com.example.motormates.ui.editProfile

import androidx.lifecycle.ViewModel
import com.example.motormates.data.mock.UserMocks
import com.example.motormates.data.model.GarageCar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * username/bio siguen el mismo patrón por-campo de LoginViewModel, sembrados
 * desde UserMocks.sampleUserProfile. cars vive en su propio flow porque no es
 * parte del formulario que se edita aquí (mismo criterio que isBookmarked en
 * VehicleDetailViewModel) — todavía no hay UI para agregar/quitar vehículos.
 */
class EditProfileViewModel : ViewModel() {

    private val _username = MutableStateFlow(UserMocks.sampleUserProfile.name)
    val username: StateFlow<String> = _username

    private val _bio = MutableStateFlow(UserMocks.sampleUserProfile.bio)
    val bio: StateFlow<String> = _bio

    private val _cars = MutableStateFlow(UserMocks.sampleUserCars)
    val cars: StateFlow<List<GarageCar>> = _cars

    fun updateUsername(input: String) {
        _username.value = input
    }

    fun updateBio(input: String) {
        _bio.value = input
    }
}
