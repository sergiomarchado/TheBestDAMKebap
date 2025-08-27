package com.sergiom.thebestdamkebap.view.products.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.material3.FilterChipDefaults.filterChipBorder
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.DialogProperties

/**
 * UI de personalización de un producto: permitir quitar ingredientes.
 * - Chips seleccionados = ingrediente quitado (usa errorContainer para diferenciar).
 * - Acciones rápidas: Quitar todo / Restaurar.
 *
 * Extra: el **primer ingrediente** se considera base y **no puede quitarse**.
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

    // Ingrediente base (no removible): el primero de la lista, si existe
    val baseIngredient = remember(product.id) { product.ingredients.firstOrNull() }
    val totalRemovibles = remember(product.id) {
        (product.ingredients.size - if (baseIngredient != null) 1 else 0).coerceAtLeast(0)
    }

    // Estado de ingredientes quitados (persistente) — saneado para no incluir el base
    val removed = rememberSaveable(
        product.id,
        saver = listSaver(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) {
        mutableStateListOf<String>().apply {
            val initialClean = initial?.removedIngredients?.filter { it != baseIngredient }.orEmpty()
            addAll(initialClean)
        }
    }

    val basePrice = remember(product.id, mode) {
        when (mode) {
            OrderMode.DELIVERY -> product.prices.delivery ?: product.prices.pickup
            OrderMode.PICKUP, null -> product.prices.pickup ?: product.prices.delivery
        }
    }
    val priceLabel = basePrice?.toPriceLabel()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Ancho responsive del diálogo (más ancho en móvil, con topes en pantallas grandes)
        BoxWithConstraints {
            val widthModifier =
                if (maxWidth < 720.dp) {
                    // Teléfonos: casi a pantalla completa
                    Modifier.fillMaxWidth(0.98f)
                } else {
                    // Tablets / landscape: aire lateral, con mínimos y máximos
                    Modifier
                        .fillMaxWidth(0.90f)
                        .widthIn(min = 720.dp, max = 1080.dp)
                }

            Surface(
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 6.dp,
                modifier = widthModifier
                    .padding(horizontal = 8.dp, vertical = 12.dp)
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
                                val allRemoved = removed.size == totalRemovibles
                                val noneRemoved = removed.isEmpty()

                                AssistChip(
                                    onClick = {
                                        removed.clear()
                                        // quitar todos los que SÍ son removibles (excluye base)
                                        removed.addAll(product.ingredients.filter { it != baseIngredient })
                                    },
                                    enabled = !allRemoved && totalRemovibles > 0,
                                    label = { Text("Quitar todo") }
                                )
                                AssistChip(
                                    onClick = { removed.clear() },
                                    enabled = !noneRemoved,
                                    label = { Text("Restaurar") }
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    "${removed.size}/$totalRemovibles",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Chips fluidos (selección = quitado) — el primero (base) está deshabilitado
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics {
                                        contentDescription = "Ingredientes personalizables"
                                    }
                            ) {
                                product.ingredients.forEachIndexed { index, ing ->
                                    val isBase = (index == 0) && (ing == baseIngredient)
                                    val chipEnabled = !isBase
                                    val isRemoved = chipEnabled && (ing in removed)

                                    FilterChip(
                                        selected = isRemoved,
                                        enabled = chipEnabled,
                                        onClick = {
                                            if (!chipEnabled) return@FilterChip
                                            if (isRemoved) removed.remove(ing) else removed.add(ing)
                                        },
                                        label = {
                                            Text(
                                                text = if (isBase) ing else ing,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                                            selectedTrailingIconColor = MaterialTheme.colorScheme.onErrorContainer,
                                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        border = filterChipBorder(
                                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                                            selectedBorderColor = MaterialTheme.colorScheme.error,
                                            enabled = chipEnabled,
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
}
