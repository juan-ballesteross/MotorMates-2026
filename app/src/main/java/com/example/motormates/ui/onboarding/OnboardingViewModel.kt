package com.example.motormates.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.CarRef
import com.example.motormates.data.model.Interest
import com.example.motormates.data.model.MAX_PREFERENCE_SELECTION
import com.example.motormates.data.model.UserPreferences
import com.example.motormates.data.model.Vehicle
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.FollowRepository
import com.example.motormates.data.repository.PreferencesRepository
import com.example.motormates.data.repository.ReviewRepository
import com.example.motormates.data.repository.UserRepository
import com.example.motormates.data.repository.VehicleRepository
import com.example.motormates.data.source.DeviceLocationDataSource
import com.example.motormates.data.util.CityCoordinates
import com.example.motormates.data.util.TimeSource
import com.example.motormates.data.util.UserSuggester
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val vehicleRepository: VehicleRepository,
    private val reviewRepository: ReviewRepository,
    private val followRepository: FollowRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val deviceLocation: DeviceLocationDataSource,
    private val timeSource: TimeSource
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    private var vehicles: List<Vehicle> = emptyList()
    /**
     * El onboarding ya no pregunta por el carro (no se agrega nada al
     * garaje). Si alguien lo había dicho en una versión anterior, se conserva.
     */
    private var previousCar: CarRef? = null
    private var loaded = false

    /**
     * editMode = false: viene del login o del registro. Si ya contestó (o
     * saltó) antes, termina de una vez y la pantalla sigue al feed.
     */
    fun load(editMode: Boolean) {
        if (loaded) return
        loaded = true
        _state.update { it.copy(isLoading = true, isEditMode = editMode) }

        viewModelScope.launch {
            // Con tope: si Firestore no responde, mejor dejar entrar al feed
            // que dejar al usuario mirando un indicador de carga.
            val result = withTimeoutOrNull(LOAD_TIMEOUT_MS) { preferencesRepository.getMine() }
            val preferences = result?.getOrNull()
            if (!editMode && (preferences == null || preferences.isDecided)) {
                _state.update { it.copy(isLoading = false, isFinished = true) }
                return@launch
            }
            prefill(preferences ?: UserPreferences())
            loadCatalogs()
            _state.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun prefill(preferences: UserPreferences) {
        previousCar = preferences.car
        // La ciudad del perfil ("Medellín, Colombia") sirve de punto de partida.
        val city = preferences.city ?: authRepository.getCurrentUser()?.id?.let { uid ->
            userRepository.getCurrentUserProfile(uid).getOrNull()?.location
        }?.let { CityCoordinates.findIn(it)?.name }

        _state.update {
            it.copy(
                categories = preferences.categories.toSet(),
                brands = preferences.brands.toSet(),
                interests = preferences.interests.toSet(),
                city = city
            )
        }
    }

    private suspend fun loadCatalogs() {
        vehicles = vehicleRepository.getVehicles().getOrNull().orEmpty()
        // Las marcas que hay en la app (las que tienen carros y reseñas).
        val brandOptions = vehicles.map { it.brand.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }

        _state.update { state ->
            state.copy(
                brandOptions = brandOptions,
                categoryImages = state.categoryOptions.associateWith { category ->
                    vehicles.firstOrNull { it.category == category && it.imageUrl != null }?.imageUrl
                }
            )
        }
    }

    // ---- Navegación entre pasos ----

    fun next() {
        val state = _state.value
        if (!state.canContinue) return
        if (state.isLastStep) {
            finish()
            return
        }
        val nextStep = state.steps[state.stepIndex + 1]
        _state.update { it.copy(step = nextStep, errorMessage = null) }
        if (nextStep == OnboardingStep.PEOPLE) loadSuggestions()
    }

    fun back() {
        val state = _state.value
        if (state.isFirstStep) return
        _state.update { it.copy(step = state.steps[state.stepIndex - 1], errorMessage = null) }
    }

    // ---- Respuestas ----

    fun toggleCategory(category: String) = _state.update {
        it.copy(categories = it.categories.toggle(category))
    }

    fun toggleBrand(brand: String) = _state.update { it.copy(brands = it.brands.toggle(brand)) }

    fun updateBrandQuery(query: String) = _state.update { it.copy(brandQuery = query) }

    fun toggleInterest(interest: Interest) = _state.update {
        it.copy(interests = it.interests.toggle(interest))
    }

    fun selectCity(city: String?) = _state.update {
        it.copy(city = if (it.city == city) null else city, errorMessage = null)
    }

    /** Lo llama la pantalla cuando ya hay permiso de ubicación. */
    fun detectCity() {
        viewModelScope.launch {
            _state.update { it.copy(isLocating = true, errorMessage = null) }
            val location = deviceLocation.lastKnownLocation()
            val city = location?.let { CityCoordinates.nearest(it.lat, it.lng) }
            _state.update {
                it.copy(
                    isLocating = false,
                    city = city?.name ?: it.city,
                    errorMessage = when {
                        location == null -> "No pudimos obtener tu ubicación"
                        city == null -> "Todavía no estamos en tu ciudad; elige la más cercana"
                        else -> null
                    }
                )
            }
        }
    }

    // ---- Personas sugeridas ----

    private fun loadSuggestions() {
        val uid = authRepository.getCurrentUser()?.id
        viewModelScope.launch {
            _state.update { it.copy(isLoadingSuggestions = true) }
            val since = timeSource.nowMillis() - SUGGESTION_WINDOW_MS
            val reviews = withTimeoutOrNull(LOAD_TIMEOUT_MS) {
                reviewRepository.observeRecentReviews(since).first().getOrNull()
            }.orEmpty()
            val following = uid?.let {
                withTimeoutOrNull(LOAD_TIMEOUT_MS) { followRepository.observeFollowingIds(it).first() }
            }.orEmpty().toSet()

            val suggestions = UserSuggester.suggest(
                reviews = reviews,
                vehicles = vehicles,
                preferences = currentAnswers(),
                currentUserId = uid,
                followingIds = following
            )
            _state.update { it.copy(suggestions = suggestions, isLoadingSuggestions = false) }
        }
    }

    /** Optimista: el botón cambia de una vez y se revierte si falla. */
    fun toggleFollow(userId: String) {
        val uid = authRepository.getCurrentUser()?.id ?: return
        val wasFollowing = userId in _state.value.followed
        _state.update { it.copy(followed = it.followed.flip(userId)) }
        viewModelScope.launch {
            val result = if (wasFollowing) {
                followRepository.unfollowUser(uid, userId)
            } else {
                followRepository.followUser(uid, userId)
            }
            result.onFailure { error ->
                _state.update {
                    it.copy(followed = it.followed.flip(userId), errorMessage = error.message)
                }
            }
        }
    }

    fun followAll() {
        _state.value.suggestions
            .map { it.userId }
            .filterNot { it in _state.value.followed }
            .forEach(::toggleFollow)
    }

    // ---- Terminar ----

    private fun currentAnswers(): UserPreferences {
        val state = _state.value
        return UserPreferences(
            categories = state.categories.toList(),
            brands = state.brands.toList(),
            interests = state.interests.toList(),
            city = state.city,
            car = previousCar
        )
    }

    fun finish() {
        if (_state.value.isSaving) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val answers = currentAnswers()
            val result = preferencesRepository.save(answers)
            if (result.isFailure) {
                _state.update {
                    it.copy(isSaving = false, errorMessage = result.exceptionOrNull()?.message)
                }
                return@launch
            }
            _state.update { it.copy(isSaving = false, isFinished = true) }
        }
    }

    /** "Ahora no": no vuelve a preguntar solo; se puede completar desde el perfil. */
    fun skip() {
        if (_state.value.isSaving) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            // Si falla, igual se deja pasar: a lo sumo se le vuelve a ofrecer.
            preferencesRepository.skip()
            _state.update { it.copy(isSaving = false, isFinished = true) }
        }
    }

    fun consumeError() = _state.update { it.copy(errorMessage = null) }

    private fun <T> Set<T>.flip(item: T): Set<T> = if (item in this) this - item else this + item

    /** Como flip, pero sin pasar del tope de opciones por pregunta. */
    private fun <T> Set<T>.toggle(item: T): Set<T> = when {
        item in this -> this - item
        size >= MAX_PREFERENCE_SELECTION -> this
        else -> this + item
    }

    private companion object {
        const val LOAD_TIMEOUT_MS = 5_000L
        const val SUGGESTION_WINDOW_MS = 30L * 24 * 60 * 60 * 1000
    }
}
