package com.example.motormates.ui.search.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import com.example.motormates.R
import com.example.motormates.data.model.SearchCategory
import com.example.motormates.data.model.SearchCategoryKey

/**
 * Empareja cada [SearchCategoryKey] con su etiqueta localizada de
 * res/values/strings.xml (mismo orden que R.array.search_categories),
 * separando la clave de negocio usada para filtrar del texto mostrado.
 */
@Composable
fun rememberSearchCategories(): List<SearchCategory> {
    val labels = stringArrayResource(R.array.search_categories)
    return SearchCategoryKey.entries.zip(labels) { key, label -> SearchCategory(key, label) }
}
