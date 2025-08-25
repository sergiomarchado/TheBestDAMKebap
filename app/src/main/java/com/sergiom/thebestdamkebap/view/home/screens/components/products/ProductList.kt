package com.sergiom.thebestdamkebap.view.home.screens.components.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.storage.FirebaseStorage
import com.sergiom.thebestdamkebap.domain.catalog.Product
import com.sergiom.thebestdamkebap.domain.order.OrderMode

@Composable
internal fun ProductList(
    products: List<Product>,
    mode: OrderMode?,
    storage: FirebaseStorage
) {
    Text(
        text = "Productos:",
        style = MaterialTheme.typography.titleLarge,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, start = 12.dp)
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        items(products, key = { it.id }) { p ->
            ProductCard(p, mode, storage)
        }
    }
}
