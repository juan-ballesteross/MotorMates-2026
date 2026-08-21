package com.example.motormates.ui.search.components

import androidx.compose.foundation.background
<<<<<<< HEAD
=======
import androidx.compose.foundation.clickable
>>>>>>> origin/master
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.R
import com.example.motormates.ui.search.model.CarListing
import com.example.motormates.ui.theme.MotorMatesRed
import com.example.motormates.ui.theme.MotorMatesSurface
import com.example.motormates.ui.theme.MotorMatesTextSecondary

@Composable
<<<<<<< HEAD
fun CarResultsGrid(cars: List<CarListing>, modifier: Modifier = Modifier) {
=======
fun CarResultsGrid(
    cars: List<CarListing>,
    onCarClick: (CarListing) -> Unit = {},
    modifier: Modifier = Modifier
) {
>>>>>>> origin/master
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        gridItems(cars) { car ->
<<<<<<< HEAD
            CarResultCard(car)
=======
            CarResultCard(car = car, onClick = { onCarClick(car) })
>>>>>>> origin/master
        }
    }
}

@Composable
<<<<<<< HEAD
private fun CarResultCard(car: CarListing, modifier: Modifier = Modifier) {
=======
private fun CarResultCard(
    car: CarListing,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
>>>>>>> origin/master
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MotorMatesSurface)
<<<<<<< HEAD
=======
            .clickable(onClick = onClick)
>>>>>>> origin/master
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.DirectionsCar,
                contentDescription = null,
                tint = MotorMatesTextSecondary,
                modifier = Modifier.size(40.dp)
            )
        }
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = stringResource(R.string.search_car_title, car.brand, car.model),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.search_car_year_owner, car.year, car.ownerName),
                color = MotorMatesTextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = MotorMatesRed, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "${car.rating}", color = MotorMatesTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Icon(Icons.Filled.FavoriteBorder, contentDescription = null, tint = MotorMatesTextSecondary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "${car.likes}", color = MotorMatesTextSecondary, fontSize = 12.sp)
            }
        }
    }
}
