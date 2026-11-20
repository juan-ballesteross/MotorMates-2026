package com.example.motormates.ui.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import com.example.motormates.R
import com.example.motormates.data.model.CarListing
import com.example.motormates.ui.common.components.ShimmerAsyncImage
import com.example.motormates.ui.theme.Elevation
import com.example.motormates.ui.theme.Radius
import com.example.motormates.ui.theme.pressScaleClickable

@Composable
fun CarResultsGrid(
    cars: List<CarListing>,
    onCarClick: (CarListing) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        gridItems(cars) { car ->
            CarResultCard(car = car, onClick = { onCarClick(car) })
        }
    }
}

@Composable
private fun CarResultCard(
    car: CarListing,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = Elevation.sm, shape = RoundedCornerShape(Radius.lg))
            .clip(RoundedCornerShape(Radius.lg))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Radius.lg))
            .pressScaleClickable(onClick = onClick)
    ) {
        ShimmerAsyncImage(
            model = car.imageUrl,
            contentDescription = stringResource(R.string.search_car_title, car.brand, car.model),
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        )
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = stringResource(R.string.search_car_title, car.brand, car.model),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                // Con dueño: "2024 · Rodrigo"; del catálogo: "2024 · Deportivo".
                text = stringResource(R.string.search_car_year_owner, car.year, car.ownerName ?: car.categoryLabel),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
