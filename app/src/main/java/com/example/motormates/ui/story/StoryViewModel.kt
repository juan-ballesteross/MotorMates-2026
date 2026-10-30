package com.example.motormates.ui.story

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.StoryUser
import com.example.motormates.data.model.toStoryUser
import com.example.motormates.data.model.toUserSearchResult
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.FollowRepository
import com.example.motormates.data.repository.StoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class StoryViewModel @Inject constructor(
    private val storyRepository: StoryRepository,
    private val authRepository: AuthRepository,
    private val followRepository: FollowRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StoryUiState())
    val uiState: StateFlow<StoryUiState> = _uiState

    private var stories: List<StoryUser> = emptyList()
    private var likeJob: Job? = null

    fun loadStory(index: Int) {
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUser()?.id
            if (stories.isEmpty()) {
                val followingIds = currentUserId?.let { followRepository.observeFollowingIds(it).first() }.orEmpty()
                val visibleUserIds = followingIds.toSet() + currentUserId.orEmpty()
                stories = storyRepository.observeActiveStories().first()
                    .filter { it.userId in visibleUserIds }
                    .map { it.toStoryUser() }
            }
            val story = stories.getOrNull(index)?.takeIf { it.imageUrl != null }
            _uiState.update {
                it.copy(
                    story = story,
                    totalStories = stories.size,
                    isOwnStory = story != null && story.userId == currentUserId
                )
            }
            observeLikeData(story?.id)
        }
    }

    private fun observeLikeData(storyId: String?) {
        likeJob?.cancel()
        if (storyId == null) {
            _uiState.update { it.copy(isLiked = false, likesCount = 0L) }
            return
        }
        likeJob = viewModelScope.launch {
            combine(
                storyRepository.observeIsLiked(storyId),
                storyRepository.observeLikesCount(storyId)
            ) { liked, count -> liked to count }
                .collect { (liked, count) ->
                    _uiState.update { it.copy(isLiked = liked, likesCount = count) }
                }
        }
    }

    fun likeButtonPress() {
        val story = _uiState.value.story ?: return
        val wasLiked = _uiState.value.isLiked
        _uiState.update { it.copy(isLiked = !wasLiked) }

        viewModelScope.launch {
            val result = if (wasLiked) {
                storyRepository.unlikeStory(story.id)
            } else {
                storyRepository.likeStory(story.id, story.userId)
            }
            result.onFailure {
                _uiState.update { current -> current.copy(isLiked = wasLiked) }
            }
        }
    }

    fun askShowLikers() {
        val story = _uiState.value.story ?: return
        _uiState.update { it.copy(showLikers = true, isLoadingLikers = true) }

        viewModelScope.launch {
            val likers = storyRepository.observeLikers(story.id).first().map { it.toUserSearchResult() }
            _uiState.update { it.copy(likers = likers, isLoadingLikers = false) }
        }
    }

    fun dismissLikers() {
        _uiState.update { it.copy(showLikers = false) }
    }

    fun nextIndexWithContent(from: Int): Int? =
        (from until stories.size).firstOrNull { stories[it].imageUrl != null }

    fun previousIndexWithContent(from: Int): Int? =
        (from downTo 0).firstOrNull { stories[it].imageUrl != null }

    fun askDeleteStory() {
        _uiState.update { it.copy(pendingDelete = true) }
    }

    fun dismissDeleteStory() {
        _uiState.update { it.copy(pendingDelete = false) }
    }

    fun confirmDeleteStory(onDeleted: () -> Unit) {
        val storyId = _uiState.value.story?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(pendingDelete = false) }
            val result = storyRepository.deleteStory(storyId)
            result.fold(
                onSuccess = {
                    stories = stories.filterNot { it.id == storyId }
                    onDeleted()
                },
                onFailure = { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
            )
        }
    }

    fun updateStory(storyId: String, imageUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, errorMessage = null) }
            val result = storyRepository.updateStory(storyId, imageUri)
            result.fold(
                onSuccess = { newImageUrl ->
                    stories = stories.map { if (it.id == storyId) it.copy(imageUrl = newImageUrl) else it }
                    _uiState.update { current ->
                        val updatedStory = if (current.story?.id == storyId) {
                            current.story.copy(imageUrl = newImageUrl)
                        } else {
                            current.story
                        }
                        current.copy(story = updatedStory, isUpdating = false)
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isUpdating = false, errorMessage = error.message) }
                }
            )
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
