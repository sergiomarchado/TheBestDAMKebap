package com.sergiom.thebestdamkebap.view.products.components.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.storage.FirebaseStorage
import com.sergiom.thebestdamkebap.core.imageloading.StorageImage
import com.sergiom.thebestdamkebap.domain.catalog.Product
import com.sergiom.thebestdamkebap.domain.order.OrderMode
import com.sergiom.thebestdamkebap.view.products.components.products.utils.priceFor
import com.sergiom.thebestdamkebap.view.products.components.products.utils.toPriceLabel

@Composable
internal fun ProductCard(
    product: Product,
    mode: OrderMode?,
    storage: FirebaseStorage,
    onClick: (() -> Unit)? = null
) {
    val priceLabel = product.priceFor(mode).toPriceLabel()

    val border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    val shape = MaterialTheme.shapes.large

    if (onClick != null) {
        Card(
            onClick = onClick,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = border,
            modifier = Modifier.fillMaxWidth()
        ) {
            ProductCardContent(product, priceLabel, storage)
        }
    } else {
        Card(
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = border,
            modifier = Modifier.fillMaxWidth()
        ) {
            ProductCardContent(product, priceLabel, storage)
        }
    }
}

@Composable
private fun ProductCardContent(
    product: Product,
    priceLabel: String,
    storage: FirebaseStorage
) {
    Row(Modifier.fillMaxWidth()) {

        // Imagen
        Box(
            Modifier
                .size(96.dp)
                .align(Alignment.CenterVertically)
        ) {
            val path = product.imagePath
            if (!path.isNullOrBlank()) {
                val ref = remember(path) { storage.reference.child(path) }
                StorageImage(
                    ref = ref,
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.medium), // recorte suave
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("IMG") }
            }
        }

        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                product.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline
            )

            if (!product.description.isNullOrBlank()) {
                Text(
                    product.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(6.dp))

            // Precio a la derecha
            Box(Modifier.fillMaxWidth()) {
                Text(
                    priceLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
    }
}
