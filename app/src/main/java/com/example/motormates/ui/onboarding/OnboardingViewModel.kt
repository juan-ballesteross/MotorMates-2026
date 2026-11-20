package com.example.motormates.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.CarRef
import com.example.motormates.data.model.Interest
import com.example.motormates.data.model.MAX_PREFERENCE_SELECTION
import com.example.motormates.data.model.UserPreferences
import com.example.motormates.data.model.Vehicle
import com.example.motormates.data.model.VehicleDraft
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.CarCatalogRepository
import com.example.motormates.data.repository.FollowRepository
import com.example.motormates.data.repository.GarageRepository
import com.example.motormates.data.repository.PreferencesRepository
import com.example.motormates.data.repository.ReviewRepository
import com.example.motormates.data.repository.UserRepository
import com.example.motormates.data.repository.VehicleRepository
import com.example.motormates.data.source.DeviceLocationDataSource
import com.example.motormates.data.util.CityCoordinates
import com.example.motormates.data.util.TimeSource
import com.example.motormates.data.util.UserSuggester
import com.example.motormates.data.util.normalizedKey
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val carCatalogRepository: CarCatalogRepository,
    private val garageRepository: GarageRepository,
    private val deviceLocation: DeviceLocationDataSource,
    private val timeSource: TimeSource
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    private var vehicles: List<Vehicle> = emptyList()
    /** El carro que ya tenía guardado: al editar no se vuelve a publicar. */
    private var previousCar: CarRef? = null
    private var loaded = false
    private var modelsJob: Job? = null

    /**
     * editMode = false: viene del login o del registro. Si ya contestó (o
     * saltó) antes, termina de una vez y la pantalla sigue al feed.
     */
    fun load(editMode: Boolean) {
        if (loaded) return
        loaded = true
        val currentYear = Instant.ofEpochMilli(timeSource.nowMillis()).atZone(ZoneId.systemDefault()).year
        _state.update { it.copy(isLoading = true, isEditMode = editMode, currentYear = currentYear) }

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
                city = city,
                hasCar = preferences.car?.let { true } ?: if (preferences.completed) false else null,
                carBrand = preferences.car?.brand.orEmpty(),
                carModel = preferences.car?.model.orEmpty(),
                carYear = preferences.car?.year?.toString().orEmpty(),
                carCategory = preferences.categories.firstOrNull()
            )
        }
        preferences.car?.let { loadCarModels(it.brand) }
    }

    private suspend fun loadCatalogs() {
        vehicles = vehicleRepository.getVehicles().getOrNull().orEmpty()
        val catalogBrands = runCatching { carCatalogRepository.brands() }.getOrDefault(emptyList())
        // Las marcas del catálogo de la app primero: son las que tienen reseñas.
        val vehicleBrands = vehicles.map { it.brand }
        val brandOptions = (vehicleBrands + catalogBrands)
            .distinctBy { it.normalizedKey() }
            .sortedWith(compareBy<String> { brand -> vehicleBrands.none { it.normalizedKey() == brand.normalizedKey() } }
                .thenBy { it.normalizedKey() })

        _state.update { state ->
            state.copy(
                brandOptions = brandOptions,
                categoryImages = state.categoryOptions.associateWith { category ->
                    vehicles.firstOrNull { it.category == category && it.imageUrl != null }?.imageUrl
                }
            )
        }
        refreshCarAlreadyInGarage()
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

    fun setHasCar(hasCar: Boolean) = _state.update {
        it.copy(hasCar = hasCar, carCategory = it.carCategory ?: it.categories.firstOrNull())
    }

    fun updateCarBrand(brand: String) {
        _state.update { it.copy(carBrand = brand) }
        loadCarModels(brand)
        refreshCarAlreadyInGarage()
    }

    fun updateCarModel(model: String) {
        _state.update { it.copy(carModel = model) }
        refreshCarAlreadyInGarage()
    }

    fun updateCarYear(year: String) {
        val previousYear = _state.value.carYearNumber
        _state.update { it.copy(carYear = year.filter(Char::isDigit).take(4)) }
        // Con el año completo vPIC puede responder por ese año en particular.
        if (_state.value.carYearNumber != previousYear) loadCarModels(_state.value.carBrand)
        refreshCarAlreadyInGarage()
    }

    fun selectCarCategory(category: String) = _state.update { it.copy(carCategory = category) }

    /** Modelos de la marca: lista local más vPIC, con un pequeño respiro mientras escribe. */
    private fun loadCarModels(brand: String) {
        modelsJob?.cancel()
        if (brand.isBlank()) {
            _state.update { it.copy(carModelOptions = emptyList(), isLoadingCarModels = false) }
            return
        }
        modelsJob = viewModelScope.launch {
            _state.update { it.copy(isLoadingCarModels = true) }
            delay(MODEL_LOOKUP_DEBOUNCE_MS)
            val year = _state.value.carYearNumber ?: _state.value.currentYear
            val options = carCatalogRepository.models(brand, year)
            _state.update { it.copy(carModelOptions = options, isLoadingCarModels = false) }
        }
    }

    private fun refreshCarAlreadyInGarage() {
        val uid = authRepository.getCurrentUser()?.id
        _state.update { state ->
            val year = state.carYearNumber
            state.copy(
                carAlreadyInGarage = uid != null && year != null && vehicles.any { vehicle ->
                    vehicle.ownerId == uid &&
                        vehicle.brand.normalizedKey() == state.carBrand.normalizedKey() &&
                        vehicle.model.normalizedKey() == state.carModel.normalizedKey() &&
                        vehicle.year == year
                }
            )
        }
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
        val car = if (state.hasCar == true && state.isCarComplete) {
            CarRef(state.carBrand.trim(), state.carModel.trim(), state.carYearNumber!!)
        } else {
            null
        }
        return UserPreferences(
            categories = state.categories.toList(),
            brands = state.brands.toList(),
            interests = state.interests.toList(),
            city = state.city,
            car = car
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
            // Agregar el carro al garaje es un extra: si falla, las
            // preferencias ya quedaron y el usuario lo puede publicar luego.
            // Solo un carro nuevo: el que ya había declarado se publicó en su
            // momento (y si lo borró del garaje, fue a propósito).
            val carError = answers.car
                ?.takeIf { it != previousCar && !_state.value.carAlreadyInGarage }
                ?.let { car -> addCarToGarage(car) }
            _state.update { it.copy(isSaving = false, isFinished = carError == null, errorMessage = carError) }
            if (carError != null) {
                // Se muestra el aviso y se sigue: el usuario no tiene que repetir nada.
                delay(CAR_ERROR_VISIBLE_MS)
                _state.update { it.copy(isFinished = true) }
            }
        }
    }

    private suspend fun addCarToGarage(car: CarRef): String? {
        val category = _state.value.carCategory ?: return null
        val draft = VehicleDraft(
            brand = car.brand,
            model = car.model,
            year = car.year,
            category = category,
            potencia = null,
            aceleracion = null,
            velocidadMaxima = null,
            traccion = null
        )
        return garageRepository.publishVehicle(draft, imageUri = null)
            .exceptionOrNull()
            ?.let { "Tus gustos quedaron guardados, pero no pudimos agregar tu carro al garaje. Puedes publicarlo desde tu perfil." }
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
        const val MODEL_LOOKUP_DEBOUNCE_MS = 300L
        const val CAR_ERROR_VISIBLE_MS = 2_500L
        const val SUGGESTION_WINDOW_MS = 30L * 24 * 60 * 60 * 1000
    }
}
