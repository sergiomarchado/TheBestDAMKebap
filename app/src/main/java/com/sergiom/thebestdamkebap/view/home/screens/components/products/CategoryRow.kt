package com.sergiom.thebestdamkebap.view.home.screens.components.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sergiom.thebestdamkebap.domain.catalog.Category

@Composable
internal fun CategoryRow(
    categories: List<Category>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories, key = { it.id }) { cat ->
            FilterChip(
                selected = cat.id == selectedId,
                onClick = { onSelect(cat.id) },
                label = { Text(cat.name) }
            )
        }
    }
}
