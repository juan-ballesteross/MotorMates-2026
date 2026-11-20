package com.example.motormates.ui.findPart

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.TuningImage
import com.example.motormates.data.repository.AiTuningRepository
import com.example.motormates.navigation.ScreenRoute
import com.example.motormates.ui.aiTuning.AiTaskManager
import com.example.motormates.ui.aiTuning.FindPartDraft
import com.example.motormates.ui.aiTuning.GroundedUi
import com.example.motormates.ui.aiTuning.TaskStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FindPartState(
    val image: TuningImage? = null,
    val description: String = "",
    val isPreparing: Boolean = false,
    /** null: todavía no se ha buscado. */
    val search: GroundedUi? = null,
    val message: String? = null,
    /** "Modificar mi carro con esta pieza": la pantalla navega a AI Tuning. */
    val openTuning: Boolean = false,
    val isFinished: Boolean = false
) {
    val canSearch: Boolean get() = image != null && !isPreparing && search?.isLoading != true
}

/**
 * "Encontrar pieza": una foto (cámara o galería), se le quita el fondo en el
 * teléfono y Gemini la busca en Google. La búsqueda corre en AiTaskManager:
 * el usuario puede salir y le llega una notificación con el resultado.
 */
@HiltViewModel
class FindPartViewModel @Inject constructor(
    private val repository: AiTuningRepository,
    private val taskManager: AiTaskManager
) : ViewModel() {

    private val local = MutableStateFlow(FindPartState())

    init {
        // La foto, la descripción y el resultado viven en el manager: al
        // volver desde la notificación la pantalla los encuentra igual.
        viewModelScope.launch {
            combine(taskManager.findPartDraft, taskManager.searchTasks) { draft, tasks ->
                draft to tasks[AiTaskManager.FIND_PART_KEY]
            }.collect { (draft, task) ->
                local.update {
                    it.copy(
                        image = draft.image,
                        description = draft.description,
                        search = task?.let { t ->
                            GroundedUi(
                                isLoading = t.status == TaskStatus.RUNNING,
                                answer = t.answer,
                                error = t.error
                            )
                        }
                    )
                }
            }
        }
    }

    val state: StateFlow<FindPartState> = local.asStateFlow()

    fun setScreenVisible(visible: Boolean) =
        taskManager.setScreenVisible(AiTaskManager.SCREEN_FIND_PART, visible)

    override fun onCleared() {
        taskManager.setScreenVisible(AiTaskManager.SCREEN_FIND_PART, false)
    }

    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch {
            local.update { it.copy(isPreparing = true) }
            taskManager.clearSearch(AiTaskManager.FIND_PART_KEY)
            repository.prepareOwnPart(uri)
                .onSuccess { image ->
                    taskManager.updateFindPartDraft { it.copy(image = image) }
                    local.update { it.copy(isPreparing = false) }
                }
                .onFailure { error -> local.update { it.copy(isPreparing = false, message = error.message) } }
        }
    }

    fun updateDescription(text: String) =
        taskManager.updateFindPartDraft { it.copy(description = text.take(MAX_TEXT)) }

    fun removePhoto() {
        taskManager.clearSearch(AiTaskManager.FIND_PART_KEY)
        taskManager.updateFindPartDraft { it.copy(image = null) }
    }

    fun search() {
        val state = local.value
        val image = state.image ?: return
        if (!state.canSearch) return
        val description = state.description
        taskManager.startSearch(
            key = AiTaskManager.FIND_PART_KEY,
            screen = AiTaskManager.SCREEN_FIND_PART,
            progressTitle = "Buscando tu pieza en Google…",
            doneTitle = "Encontramos tu pieza",
            route = ScreenRoute.FindPart.route
        ) { repository.identifyPart(image, description) }
    }

    /** Empezar con otra pieza: se borra todo. */
    fun newSearch() {
        taskManager.clearSearch(AiTaskManager.FIND_PART_KEY)
        taskManager.updateFindPartDraft { FindPartDraft() }
    }

    /** Llevar la pieza a AI Tuning (modo "Tu pieza", con la foto ya cargada). */
    fun tuneWithPart() {
        val image = local.value.image ?: return
        taskManager.handOffPartImage(image)
        local.update { it.copy(openTuning = true) }
    }

    fun onTuningOpened() = local.update { it.copy(openTuning = false) }

    fun finish() {
        newSearch()
        local.update { it.copy(isFinished = true) }
    }

    fun consumeMessage() = local.update { it.copy(message = null) }

    private companion object {
        const val MAX_TEXT = 200
    }
}
