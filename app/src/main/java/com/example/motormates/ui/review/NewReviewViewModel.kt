package com.example.motormates.ui.review

import androidx.lifecycle.ViewModel
import com.example.motormates.data.model.ReviewAspect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class NewReviewUiState(
    val rating: Int = 4,
    val experience: String = "",
    val selectedAspects: Set<ReviewAspect> = setOf(ReviewAspect.COMFORT, ReviewAspect.PERFORMANCE),
    val photoCount: Int = 0
)

class NewReviewViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(NewReviewUiState())
    val uiState: StateFlow<NewReviewUiState> = _uiState

    fun updateRating(rating: Int) {
        _uiState.update { it.copy(rating = rating) }
    }

    fun updateExperience(input: String) {
        _uiState.update { it.copy(experience = input) }
    }

    fun toggleAspect(aspect: ReviewAspect) {
        _uiState.update { current ->
            val selectedAspects = if (aspect in current.selectedAspects) {
                current.selectedAspects - aspect
            } else {
                current.selectedAspects + aspect
            }
            current.copy(selectedAspects = selectedAspects)
        }
    }

    fun addPhotoButtonPress() {
        _uiState.update { current ->
            if (current.photoCount < 3) {
                current.copy(photoCount = current.photoCount + 1)
            } else {
                current
            }
        }
    }
}
