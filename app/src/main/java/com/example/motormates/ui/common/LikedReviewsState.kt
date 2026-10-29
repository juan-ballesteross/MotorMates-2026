package com.example.motormates.ui.common

import com.example.motormates.data.repository.LikeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update

/**
 * El "ya le di like" de una lista de reseñas, en tiempo real y con
 * respuesta inmediata al tocar el corazón. Lo comparten el detalle, la
 * lista de comentarios y el feed de "Siguiendo", que antes repetían la
 * misma lógica y solo consultaban los likes una vez.
 *
 * - Lo que dice el servidor llega de LikeRepository.observeLikedReviewIds
 *   (listener en Firestore, polling en el backend propio), así que un like
 *   dado o quitado desde otro dispositivo también se ve.
 * - Al tocar el corazón se guarda un cambio pendiente que se muestra de
 *   inmediato. Se descarta cuando el servidor confirma ese mismo valor, o
 *   cuando la escritura falla (y el corazón vuelve a como estaba).
 */
class LikedReviewsState(private val likeRepository: LikeRepository) {

    /** reviewId -> liked que el usuario acaba de pedir y el servidor no ha confirmado. */
    private val pending = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    /**
     * Cada vez que cambia la lista de reseñas se reabre la escucha de likes
     * (flatMapLatest bota la anterior). distinctUntilChanged evita reabrirla
     * cuando solo cambió, por ejemplo, el likesCount de una reseña.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(userId: String?, reviewIds: Flow<List<String>>): Flow<Set<String>> {
        if (userId == null) return flowOf(emptySet())

        val confirmed = reviewIds
            .distinctUntilChanged()
            .flatMapLatest { ids -> likeRepository.observeLikedReviewIds(userId, ids) }

        return combine(confirmed, pending) { server, overrides ->
            val settled = overrides.filter { (id, liked) -> (id in server) == liked }.keys
            if (settled.isNotEmpty()) pending.update { it - settled }

            overrides.entries.fold(server) { liked, (id, isLiked) ->
                if (isLiked) liked + id else liked - id
            }
        }.distinctUntilChanged()
    }

    fun setPending(reviewId: String, liked: Boolean) {
        pending.update { it + (reviewId to liked) }
    }

    /** La escritura falló: se vuelve a mostrar lo que diga el servidor. */
    fun discardPending(reviewId: String) {
        pending.update { it - reviewId }
    }
}
