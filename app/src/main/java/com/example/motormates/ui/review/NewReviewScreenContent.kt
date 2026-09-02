package com.example.motormates.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.R
import com.example.motormates.data.model.ReviewAspect
import com.example.motormates.data.model.ReviewCarSummary
import com.example.motormates.ui.review.components.ReviewAddPhotoButton
import com.example.motormates.ui.review.components.ReviewAspectChips
import com.example.motormates.ui.review.components.ReviewCarCard
import com.example.motormates.ui.review.components.ReviewExperienceField
import com.example.motormates.ui.review.components.ReviewStarRating

/**
 * Contenido stateless de "Nueva reseña". Layout y espaciado alineados al mockup.
 */
@Composable
fun NewReviewScreenContent(
    car: ReviewCarSummary,
    rating: Int,
    onRatingChange: (Int) -> Unit,
    experience: String,
    onExperienceChange: (String) -> Unit,
    selectedAspects: Set<ReviewAspect>,
    onToggleAspect: (ReviewAspect) -> Unit,
    photoCount: Int,
    onAddPhotoClick: () -> Unit,
    canPublish: Boolean,
    onPublishClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        ReviewCarCard(car = car)

        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.review_rating_prompt),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        ReviewStarRating(
            rating = rating,
            onRatingChange = onRatingChange,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.review_experience_label),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        ReviewExperienceField(value = experience, onValueChange = onExperienceChange)

        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.review_aspects_label),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(14.dp))
        ReviewAspectChips(selected = selectedAspects, onToggle = onToggleAspect)

        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.review_photos_label),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(14.dp))
        ReviewAddPhotoButton(photoCount = photoCount, onClick = onAddPhotoClick)

        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = onPublishClick,
            enabled = canPublish,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = stringResource(R.string.review_publish_button),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(28.dp))
    }
}
