package com.sergiom.thebestdamkebap.view.cart.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage
import com.sergiom.thebestdamkebap.core.imageloading.StorageImage
import com.sergiom.thebestdamkebap.domain.cart.CartItem
import com.sergiom.thebestdamkebap.domain.cart.MenuLine
import com.sergiom.thebestdamkebap.domain.cart.ProductLine
import java.text.NumberFormat

@Composable
internal fun CartItemRow(
    item: CartItem,
    onInc: () -> Unit,
    onDec: () -> Unit,
    onRemove: () -> Unit,
    nf: NumberFormat,
    productNameProvider: (suspend (String) -> String?)?
) {
    // Firebase Storage (memoizado por bucket e imagen)
    val bucket = remember { FirebaseApp.getInstance().options.storageBucket }
    val storage = remember(bucket) {
        bucket?.takeIf { it.isNotBlank() }?.let { FirebaseStorage.getInstance("gs://$it") }
            ?: FirebaseStorage.getInstance()
    }

    val cardShape = MaterialTheme.shapes.extraLarge

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            // Borde del card (si lo prefieres sutil, usa outlineVariant)
            .border(1.dp, MaterialTheme.colorScheme.primary, cardShape),
        shape = cardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!item.imagePath.isNullOrBlank()) {
                    val ref = remember(item.imagePath) { storage.reference.child(item.imagePath!!) }
                    StorageImage(
                        ref = ref,
                        contentDescription = item.name,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    when (item) {
                        is ProductLine -> {
                            if (item.customization?.removedIngredients?.isNotEmpty() == true) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Sin: " + item.customization.removedIngredients.joinToString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        is MenuLine -> {
                            Spacer(Modifier.height(4.dp))
                            MenuSelectionsSummary(
                                line = item,
                                productNameProvider = productNameProvider
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Precio a la izquierda
                Text(
                    nf.format(item.unitPriceCents / 100.0),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                // Stepper cantidad
                OutlinedButton(
                    onClick = onDec,
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(40.dp)
                ) { Icon(Icons.Outlined.Remove, contentDescription = "Disminuir") }

                Text(
                    "${item.qty}",
                    modifier = Modifier.padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.bodyLarge
                )

                OutlinedButton(
                    onClick = onInc,
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(40.dp)
                ) { Icon(Icons.Outlined.Add, contentDescription = "Aumentar") }

                Spacer(Modifier.width(8.dp))

                // Eliminar (acción destructiva)
                TextButton(
                    onClick = onRemove,
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Eliminar")
                }
            }
        }
    }
}
