package com.sergiom.thebestdamkebap.view.products.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilterChipDefaults.filterChipBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.firebase.Firebase
import com.google.firebase.app
import com.google.firebase.storage.FirebaseStorage
import com.sergiom.thebestdamkebap.core.imageloading.StorageImage
import com.sergiom.thebestdamkebap.domain.catalog.Product
import com.sergiom.thebestdamkebap.domain.order.OrderMode
import com.sergiom.thebestdamkebap.domain.order.ProductCustomization
import com.sergiom.thebestdamkebap.view.products.components.products.utils.toPriceLabel

/**
 * UI de personalización de un producto: permitir quitar ingredientes.
 * - Chips seleccionados = ingrediente quitado (usa errorContainer para diferenciar).
 * - Acciones rápidas: Quitar todo / Restaurar.
 *
 * @param initial  Personalización previa (si llega desde un menú), opcional.
 * @param storage  Opcional; si no se pasa, se resuelve del bucket del proyecto.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductCustomizeDialog(
    product: Product,
    mode: OrderMode?,
    initial: ProductCustomization? = null,
    storage: FirebaseStorage? = null,
    onDismiss: () -> Unit,
    onConfirm: (ProductCustomization) -> Unit
) {
    // Resolve FirebaseStorage si no lo pasan
    val bucket = remember { Firebase.app.options.storageBucket }
    val storageResolved = remember(bucket, storage) {
        storage ?: if (bucket.isNullOrBlank()) FirebaseStorage.getInstance()
        else FirebaseStorage.getInstance("gs://$bucket")
    }

    // Estado de ingredientes quitados
    val removed = remember(product.id) {
        mutableStateListOf<String>().apply {
            initial?.removedIngredients?.let { addAll(it) }
        }
    }

    val basePrice = remember(product.id, mode) {
        when (mode) {
            OrderMode.DELIVERY -> product.prices.delivery ?: product.prices.pickup
            OrderMode.PICKUP, null -> product.prices.pickup ?: product.prices.delivery
        }
    }
    val priceLabel = basePrice?.toPriceLabel()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .widthIn(max = 720.dp)
        ) {
            Column(Modifier.fillMaxWidth()) {
                // Header compacto con miniatura + título + precio
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 4.dp, top = 14.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Miniatura redonda (si hay imagen)
                    if (!product.imagePath.isNullOrBlank()) {
                        val ref = remember(product.imagePath) {
                            storageResolved.reference.child(product.imagePath)
                        }
                        StorageImage(
                            ref = ref,
                            contentDescription = product.name,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                        )
                        Spacer(Modifier.width(12.dp))
                    }

                    Column(Modifier.weight(1f)) {
                        Text(
                            product.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (priceLabel != null) {
                            Text(
                                priceLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cerrar")
                    }
                }

                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

                // Texto guía + acciones rápidas
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DisableSelection {
                        Text(
                            "Personaliza tu producto:",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Quita los ingredientes que no quieres",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (product.ingredients.isEmpty()) {
                        Text(
                            "Este producto no tiene ingredientes personalizables.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(
                                onClick = {
                                    removed.clear()
                                    removed.addAll(product.ingredients)
                                },
                                label = { Text("Quitar todo") }
                            )
                            AssistChip(
                                onClick = { removed.clear() },
                                label = { Text("Restaurar") }
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "${removed.size}/${product.ingredients.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Chips fluidos (selección = quitado)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            product.ingredients.forEach { ing ->
                                val isRemoved = ing in removed
                                FilterChip(
                                    selected = isRemoved,
                                    onClick = {
                                        if (isRemoved) removed.remove(ing) else removed.add(ing)
                                    },
                                    label = { Text(ing, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                                        selectedTrailingIconColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    border = filterChipBorder(
                                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                                        selectedBorderColor = MaterialTheme.colorScheme.error,
                                        enabled = true,
                                        selected = isRemoved
                                    )
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

                // Footer acciones
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            onConfirm(ProductCustomization(removedIngredients = removed.toSet()))
                        }
                    ) { Text("Guardar") }
                }
            }
        }
    }
}
