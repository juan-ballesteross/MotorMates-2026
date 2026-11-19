package com.example.motormates.ui.compare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.CompareSide
import com.example.motormates.data.model.Review
import com.example.motormates.data.model.Vehicle
import com.example.motormates.data.model.buildVehicleComparison
import com.example.motormates.data.model.myCompareVehicleIds
import com.example.motormates.data.model.toCompareCarUi
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.FollowRepository
import com.example.motormates.data.repository.ReviewRepository
import com.example.motormates.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Compara dos vehículos usando los repositorios que ya existen: no hace
 * falta un endpoint nuevo, así que funciona igual con Firestore o con el
 * backend Express (DataBackend).
 *
 * El cálculo (quién gana, largo de las barras) está en
 * buildVehicleComparison, que es Kotlin puro y tiene sus propias pruebas.
 */
@HiltViewModel
class CompareViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val reviewRepository: ReviewRepository,
    private val authRepository: AuthRepository,
    private val followRepository: FollowRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CompareState())
    val state: StateFlow<CompareState> = _state.asStateFlow()

    /** Vehículo + reseñas ya cargados, para no volver a pedirlos al intercambiar lados. */
    private val cache = mutableMapOf<String, CarData>()

    private var started = false
    private var loadJob: Job? = null

    /**
     * Se llama desde un LaunchedEffect. Al rotar la pantalla el ViewModel
     * sobrevive y el efecto se vuelve a disparar: por eso solo cuenta la
     * primera llamada, si no se perdería el carro que el usuario ya eligió.
     */
    fun start(leftId: String, rightId: String?, compareWithMine: Boolean = false) {
        if (started) return
        started = true

        _state.update { it.copy(leftId = leftId, rightId = rightId) }
        loadComparison()

        if (rightId == null) {
            if (compareWithMine) {
                findMyCar(leftId)
            } else {
                // Entrando desde el detalle solo hay un carro: se abre de una
                // vez el selector para elegir el segundo.
                openPicker(CompareSide.RIGHT)
            }
        }
    }

    /**
     * "Comparar con el mío": si el usuario tiene un solo carro (suyo o
     * reseñado) se compara directo; si tiene varios, el selector los muestra
     * arriba; si no tiene ninguno, se abre el selector normal con un aviso.
     */
    private fun findMyCar(leftId: String) {
        val uid = authRepository.getCurrentUser()?.id
        if (uid == null) {
            openPicker(CompareSide.RIGHT)
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isFindingMyCar = true) }

            val (vehiclesResult, reviewsResult) = coroutineScope {
                val vehicles = async { vehicleRepository.getVehicles() }
                val reviews = async { reviewRepository.getReviewsByUser(uid) }
                vehicles.await() to reviews.await()
            }

            val vehicles = vehiclesResult.getOrElse {
                _state.update { it.copy(isFindingMyCar = false) }
                openPicker(CompareSide.RIGHT)
                return@launch
            }
            // Si fallan las reseñas se sigue solo con los carros a su nombre.
            val myIds = myCompareVehicleIds(vehicles, reviewsResult.getOrElse { emptyList() }, uid, leftId)

            // El catálogo ya está: se le deja al selector para no pedirlo otra vez.
            _state.update {
                it.copy(
                    isFindingMyCar = false,
                    picker = it.picker.copy(
                        allVehicles = vehicles.toPickerItems(),
                        myVehicleIds = myIds
                    )
                )
            }

            if (myIds.size == 1) {
                _state.update { it.copy(rightId = myIds.single()) }
                loadComparison()
            } else {
                // Con varios carros se abre directo en "Tus carros"; sin
                // ninguno, en la lista de personas.
                openPicker(
                    CompareSide.RIGHT,
                    notice = if (myIds.isEmpty()) {
                        "Todavía no tienes carros a tu nombre ni reseñados. Elige con cuál comparar."
                    } else {
                        "Elige cuál de tus carros quieres comparar."
                    },
                    ownerKey = if (myIds.isEmpty()) null else PickerOwner.MINE_KEY
                )
            }
        }
    }

    fun retry() = loadComparison()

    private fun loadComparison() {
        val leftId = _state.value.leftId ?: return
        val rightId = _state.value.rightId

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            // Los dos carros se piden en paralelo.
            val (leftResult, rightResult) = coroutineScope {
                val left = async { loadCar(leftId) }
                val right = rightId?.let { async { loadCar(it) } }
                left.await() to right?.await()
            }

            val error = leftResult.exceptionOrNull() ?: rightResult?.exceptionOrNull()
            if (error != null) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "No se pudo cargar la comparación"
                    )
                }
                return@launch
            }

            publish(leftResult.getOrThrow(), rightResult?.getOrThrow())
        }
    }

    private fun publish(left: CarData, right: CarData?) {
        _state.update {
            it.copy(
                isLoading = false,
                errorMessage = null,
                left = left.vehicle.toCompareCarUi(left.reviews),
                right = right?.vehicle?.toCompareCarUi(right.reviews),
                comparison = right?.let { r ->
                    buildVehicleComparison(left.vehicle, left.reviews, r.vehicle, r.reviews)
                }
            )
        }
    }

    /** Vehículo y reseñas en paralelo. Si cualquiera falla, falla la carga. */
    private suspend fun loadCar(id: String): Result<CarData> {
        cache[id]?.let { return Result.success(it) }

        return coroutineScope<Result<CarData>> {
            val vehicle = async { vehicleRepository.getVehicleById(id) }
            val reviews = async { reviewRepository.getReviewsByVehicle(id) }

            val v = vehicle.await().getOrElse { return@coroutineScope Result.failure<CarData>(it) }
            val r = reviews.await().getOrElse { return@coroutineScope Result.failure<CarData>(it) }
            Result.success(CarData(v, r).also { cache[id] = it })
        }
    }

    /** Intercambia izquierda y derecha. No consulta nada: los datos ya están en caché. */
    fun swapSides() {
        val current = _state.value
        val leftId = current.leftId ?: return
        val rightId = current.rightId ?: return
        val left = cache[rightId] ?: return
        val right = cache[leftId] ?: return

        _state.update { it.copy(leftId = rightId, rightId = leftId) }
        publish(left, right)
    }

    // ---------- Selector de vehículo ----------

    fun openPicker(side: CompareSide) = openPicker(side, notice = null, ownerKey = null)

    private fun openPicker(side: CompareSide, notice: String?, ownerKey: String?) {
        val current = _state.value
        _state.update {
            it.copy(
                picker = it.picker.copy(
                    target = side,
                    query = "",
                    notice = notice,
                    selectedOwnerKey = ownerKey,
                    myUid = authRepository.getCurrentUser()?.id,
                    excludedIds = setOfNotNull(current.leftId, current.rightId)
                )
            )
        }
        if (_state.value.picker.allVehicles.isEmpty()) loadCandidates()
        loadFollowing()
    }

    /**
     * El paso 1 del selector solo muestra a quién sigo. Se consulta cada vez
     * que se abre (pudo seguir a alguien mientras tanto). Si falla, la lista
     * queda solo con "Tus carros".
     */
    private fun loadFollowing() {
        val uid = authRepository.getCurrentUser()?.id ?: return
        viewModelScope.launch {
            val ids = followRepository.observeFollowingIds(uid).first().toSet()
            _state.update { it.copy(picker = it.picker.copy(followingIds = ids)) }
        }
    }

    fun loadCandidates() {
        viewModelScope.launch {
            _state.update { it.copy(picker = it.picker.copy(isLoading = true, errorMessage = null)) }

            vehicleRepository.getVehicles().fold(
                onSuccess = { vehicles ->
                    val items = vehicles.toPickerItems()
                    _state.update {
                        it.copy(picker = it.picker.copy(isLoading = false, allVehicles = items))
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            picker = it.picker.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "No se pudieron cargar los vehículos"
                            )
                        )
                    }
                }
            )
        }
    }

    fun onPickerQueryChange(query: String) {
        _state.update { it.copy(picker = it.picker.copy(query = query)) }
    }

    /** Paso 1 → 2: se eligió la persona; el buscador se limpia para buscar entre sus carros. */
    fun onPickerOwnerSelected(ownerKey: String) {
        _state.update { it.copy(picker = it.picker.copy(selectedOwnerKey = ownerKey, query = "")) }
    }

    /** Paso 2 → 1: volver a la lista de personas. */
    fun onPickerBack() {
        _state.update { it.copy(picker = it.picker.copy(selectedOwnerKey = null, query = "", notice = null)) }
    }

    fun dismissPicker() {
        _state.update {
            it.copy(picker = it.picker.copy(target = null, query = "", notice = null, selectedOwnerKey = null))
        }
    }

    fun selectVehicle(vehicleId: String) {
        val side = _state.value.picker.target ?: return
        _state.update {
            when (side) {
                CompareSide.LEFT -> it.copy(leftId = vehicleId)
                CompareSide.RIGHT -> it.copy(rightId = vehicleId)
            }.copy(picker = it.picker.copy(target = null, query = "", notice = null, selectedOwnerKey = null))
        }
        loadComparison()
    }

    private data class CarData(val vehicle: Vehicle, val reviews: List<Review>)
}

private fun List<Vehicle>.toPickerItems(): List<VehiclePickerItem> =
    sortedWith(compareBy({ it.brand.lowercase() }, { it.model.lowercase() }))
        .map { it.toPickerItem() }

private fun Vehicle.toPickerItem() = VehiclePickerItem(
    id = id,
    title = "$brand $model",
    subtitle = "$year · $category",
    imageUrl = imageUrl,
    ownerId = ownerId,
    ownerName = ownerName,
    ownerAvatarUrl = ownerAvatarUrl
)
