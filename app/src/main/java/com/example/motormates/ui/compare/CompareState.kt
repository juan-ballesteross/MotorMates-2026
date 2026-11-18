package com.example.motormates.ui.compare

import com.example.motormates.data.model.CompareCarUi
import com.example.motormates.data.model.CompareSide
import com.example.motormates.data.model.VehicleComparison

/**
 * Estado de la pantalla de comparación.
 *
 * left y right se guardan por separado de comparison porque al entrar
 * desde el detalle solo existe el carro de la izquierda: la cabecera ya
 * lo muestra mientras el usuario elige el segundo.
 */
data class CompareState(
    val leftId: String? = null,
    val rightId: String? = null,
    val left: CompareCarUi? = null,
    val right: CompareCarUi? = null,
    val comparison: VehicleComparison? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** "Comparar con el mío": buscando cuál es el carro del usuario. */
    val isFindingMyCar: Boolean = false,
    val picker: VehiclePickerState = VehiclePickerState()
)

/**
 * Hoja para elegir con qué carro comparar, en dos pasos: primero la persona
 * (selectedOwnerKey == null) y después uno de sus carros. target == null
 * significa cerrada.
 */
data class VehiclePickerState(
    val target: CompareSide? = null,
    /** Lo que se escribe en el buscador: filtra personas en el paso 1 y carros en el 2. */
    val query: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val allVehicles: List<VehiclePickerItem> = emptyList(),
    /** Los carros que ya están en la comparación: no se ofrecen otra vez. */
    val excludedIds: Set<String> = emptySet(),
    /** Los carros del usuario (suyos o reseñados), en orden de prioridad. Son la entrada "Tus carros". */
    val myVehicleIds: List<String> = emptyList(),
    /** Quién usa la app: sus carros van en "Tus carros", no como una persona más. */
    val myUid: String? = null,
    /** A quién sigo: solo esas personas salen en el paso 1. */
    val followingIds: Set<String> = emptySet(),
    /** Paso 2: de quién se están viendo los carros (ver PickerOwner.key). null = paso 1. */
    val selectedOwnerKey: String? = null,
    /** Aviso arriba de la lista, por ejemplo cuando el usuario todavía no tiene carros. */
    val notice: String? = null
) {
    val isOpen: Boolean get() = target != null

    /** Carros que se pueden elegir (no están ya en la comparación). */
    private val available: List<VehiclePickerItem>
        get() = allVehicles.filter { it.id !in excludedIds }

    /** Mis carros: los que me pasaron (también reseñados) más los que tengo a mi nombre. */
    private val myCarIds: List<String>
        get() = (myVehicleIds + allVehicles.filter { myUid != null && it.ownerId == myUid }.map { it.id }).distinct()

    val visibleVehicles: List<VehiclePickerItem>
        get() {
            val q = query.trim()
            return available.filter { q.isEmpty() || it.searchText.contains(q, ignoreCase = true) }
        }

    /** Sección "Tus carros", en el orden de myVehicleIds. */
    val myVisibleVehicles: List<VehiclePickerItem>
        get() {
            val byId = visibleVehicles.associateBy { it.id }
            return myVehicleIds.mapNotNull { byId[it] }
        }

    /** El resto del catálogo, sin repetir los que ya salen en "Tus carros". */
    val otherVisibleVehicles: List<VehiclePickerItem>
        get() = visibleVehicles.filter { it.id !in myVehicleIds }

    // ---------- Paso 1: personas ----------

    /**
     * Las personas que sigo y tienen al menos un carro disponible, filtradas
     * por nombre. Arriba "Tus carros"; después cada persona (A-Z); al final
     * los carros sin dueño, como "Catálogo de MotorMates".
     */
    val owners: List<PickerOwner>
        get() {
            val q = query.trim()
            val mine = myCarIds.toSet()
            val result = mutableListOf<PickerOwner>()

            val myCount = available.count { it.id in mine }
            if (myCount > 0) {
                result += PickerOwner(PickerOwner.MINE_KEY, "Tus carros", null, myCount, PickerOwner.Kind.MINE)
            }

            available
                .filter { it.ownerId != null && it.ownerId != myUid && it.ownerId in followingIds }
                .groupBy { it.ownerId!! }
                .map { (ownerId, cars) ->
                    val first = cars.first()
                    PickerOwner(ownerId, first.ownerName ?: "Usuario", first.ownerAvatarUrl, cars.size, PickerOwner.Kind.USER)
                }
                .sortedBy { it.name.lowercase() }
                .let { result += it }

            val catalogCount = available.count { it.ownerId == null }
            if (catalogCount > 0) {
                result += PickerOwner(PickerOwner.CATALOG_KEY, "Catálogo de MotorMates", null, catalogCount, PickerOwner.Kind.CATALOG)
            }

            return if (q.isEmpty()) result else result.filter { it.name.contains(q, ignoreCase = true) }
        }

    // ---------- Paso 2: carros de la persona elegida ----------

    val selectedOwner: PickerOwner?
        get() = selectedOwnerKey?.let { key ->
            // Se busca sin el filtro del buscador: en el paso 2 el texto filtra carros.
            copy(query = "").owners.firstOrNull { it.key == key }
        }

    val ownerVehicles: List<VehiclePickerItem>
        get() {
            val key = selectedOwnerKey ?: return emptyList()
            val mine = myCarIds
            return when (key) {
                PickerOwner.MINE_KEY -> {
                    val byId = visibleVehicles.associateBy { it.id }
                    mine.mapNotNull { byId[it] }
                }
                PickerOwner.CATALOG_KEY -> visibleVehicles.filter { it.ownerId == null }
                else -> visibleVehicles.filter { it.ownerId == key }
            }
        }
}

/** Una persona en el paso 1 del selector (o "Tus carros" / el catálogo). */
data class PickerOwner(
    val key: String,
    val name: String,
    val avatarUrl: String?,
    val carCount: Int,
    val kind: Kind
) {
    enum class Kind { MINE, USER, CATALOG }

    companion object {
        const val MINE_KEY = "__mine__"
        const val CATALOG_KEY = "__catalog__"
    }
}

data class VehiclePickerItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String?,
    val ownerId: String? = null,
    val ownerName: String? = null,
    val ownerAvatarUrl: String? = null
) {
    internal val searchText: String get() = "$title $subtitle"
}
