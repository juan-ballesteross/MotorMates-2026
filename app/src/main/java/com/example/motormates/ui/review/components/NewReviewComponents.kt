package com.example.motormates.ui.review.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.R
import com.example.motormates.ui.review.ReviewAspect
import com.example.motormates.ui.review.ReviewCarSummary
import com.example.motormates.ui.theme.MotorMatesRed

/** Colores locales alineados al mockup de "Nueva reseña". */
private val ReviewBlack = Color(0xFF000000)
private val ReviewSurface = Color(0xFF1C1C1E)
private val ReviewLabel = Color(0xFF8E8E93)
private val ReviewStarEmpty = Color(0xFF3A3A3C)
private val ReviewChipBorder = Color(0xFF3A3A3C)

@Composable
fun NewReviewTopBar(
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(ReviewBlack)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(R.string.review_close_cd),
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(22.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onCloseClick
                )
        )
        Text(
            text = stringResource(R.string.review_top_bar_title),
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun ReviewCarCard(
    car: ReviewCarSummary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ReviewSurface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF2C2C2E)),
            contentAlignment = Alignment.Center
        ) {
            if (car.imageResId != 0) {
                Image(
                    painter = painterResource(car.imageResId),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.DirectionsCar,
                    contentDescription = null,
                    tint = ReviewLabel,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = car.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(
                    R.string.review_car_year_category,
                    car.year,
                    car.categoryLabel
                ),
                color = ReviewLabel,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun ReviewStarRating(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        (1..5).forEach { star ->
            val selected = star <= rating
            Icon(
                imageVector = if (selected) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = stringResource(R.string.review_star_cd, star),
                tint = if (selected) MotorMatesRed else ReviewStarEmpty,
                modifier = Modifier
                    .size(40.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onRatingChange(star) }
            )
        }
    }
}

@Composable
fun ReviewExperienceField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(128.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ReviewSurface)
            .padding(16.dp)
    ) {
        if (value.isEmpty()) {
            Text(
                text = stringResource(R.string.review_experience_placeholder),
                color = ReviewLabel,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxSize(),
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 20.sp
            ),
            cursorBrush = SolidColor(MotorMatesRed)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReviewAspectChips(
    selected: Set<ReviewAspect>,
    onToggle: (ReviewAspect) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ReviewAspect.entries.forEach { aspect ->
            val isSelected = aspect in selected
            Text(
                text = aspectLabel(aspect),
                color = if (isSelected) Color.White else ReviewLabel,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isSelected) MotorMatesRed else ReviewSurface)
                    .then(
                        if (isSelected) {
                            Modifier
                        } else {
                            Modifier.border(1.dp, ReviewChipBorder, RoundedCornerShape(24.dp))
                        }
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onToggle(aspect) }
                    .padding(horizontal = 20.dp, vertical = 11.dp)
            )
        }
    }
}

/**
 * En el mockup el slot de foto es un cuadrado vacío (sin ícono +).
 */
@Composable
fun ReviewAddPhotoButton(
    photoCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ReviewSurface)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (photoCount > 0) {
            Text(
                text = "$photoCount",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun aspectLabel(aspect: ReviewAspect): String = when (aspect) {
    ReviewAspect.COMFORT -> stringResource(R.string.review_aspect_comfort)
    ReviewAspect.DESIGN -> stringResource(R.string.review_aspect_design)
    ReviewAspect.PRICE -> stringResource(R.string.review_aspect_price)
    ReviewAspect.PERFORMANCE -> stringResource(R.string.review_aspect_performance)
}
