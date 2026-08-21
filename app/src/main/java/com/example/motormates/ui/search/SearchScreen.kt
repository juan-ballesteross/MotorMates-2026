package com.example.motormates.ui.search

<<<<<<< HEAD
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
=======
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
>>>>>>> origin/master
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
<<<<<<< HEAD
import com.example.motormates.ui.common.components.MainBottomDestination
import com.example.motormates.ui.common.components.MainBottomNavBar
import com.example.motormates.ui.search.components.SearchTopBar
=======
import com.example.motormates.ui.search.components.SearchTopBar
import com.example.motormates.ui.search.model.CarListing
>>>>>>> origin/master
import com.example.motormates.ui.search.model.SearchCategoryKey
import com.example.motormates.ui.search.model.rememberSearchCategories
import com.example.motormates.ui.search.model.sampleSearchCars
import com.example.motormates.ui.theme.MotorMatesBackground

<<<<<<< HEAD
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    onFeedClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
=======
/**
 * Ya NO tiene su propio Scaffold. searchQuery/selectedCategory se quedan
 * como estado local porque solo los usa esta pantalla (el topBar de
 * búsqueda vive aquí mismo, no en el Scaffold central).
 */
@Composable
fun SearchScreen(
    onCarClick: (CarListing) -> Unit = {},
    modifier: Modifier = Modifier
>>>>>>> origin/master
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(SearchCategoryKey.ALL) }

    val categories = rememberSearchCategories()

    val filteredCars = sampleSearchCars.filter { car ->
        val matchesCategory = selectedCategory == SearchCategoryKey.ALL || car.category == selectedCategory
        val matchesQuery = searchQuery.isBlank() ||
<<<<<<< HEAD
            car.brand.contains(searchQuery, ignoreCase = true) ||
            car.model.contains(searchQuery, ignoreCase = true) ||
            car.ownerName.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesQuery
    }

    Scaffold(
        modifier = modifier,
        containerColor = MotorMatesBackground,
        topBar = {
            SearchTopBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it }
            )
        },
        bottomBar = {
            MainBottomNavBar(
                selected = MainBottomDestination.EXPLORE,
                onFeedClick = onFeedClick,
                onProfileClick = onProfileClick
            )
        }
    ) { innerPadding ->
=======
                car.brand.contains(searchQuery, ignoreCase = true) ||
                car.model.contains(searchQuery, ignoreCase = true) ||
                car.ownerName.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesQuery
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MotorMatesBackground)
    ) {
        SearchTopBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it }
        )
>>>>>>> origin/master
        SearchScreenContent(
            categories = categories,
            selectedCategory = selectedCategory,
            onSelectCategory = { selectedCategory = it },
            filteredCars = filteredCars,
<<<<<<< HEAD
            modifier = Modifier.padding(innerPadding)
=======
            onCarClick = onCarClick
>>>>>>> origin/master
        )
    }
}
