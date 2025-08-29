package com.sergiom.thebestdamkebap.view.cart.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.DeliveryDining
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sergiom.thebestdamkebap.domain.order.OrderMode
import com.sergiom.thebestdamkebap.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConfirmShippingDialog(
    currentMode: OrderMode?,               // modo actual en la sesión
    addressId: String?,                    // id actual (si hay)
    addressLabel: String?,                 // etiqueta bonita (si la has resuelto)
    onDismiss: () -> Unit,
    onPickMode: (OrderMode) -> Unit,       // fija modo en VM
    onAddAddress: () -> Unit,              // navegar a crear/seleccionar
    onChangeAddress: () -> Unit,           // navegar a gestionar
    onProceed: () -> Unit                  // abrir el diálogo de pago
) {
    // estado local para feedback inmediato
    var selected by remember(currentMode) { mutableStateOf(currentMode) }

    // Colores de los SegmentedButton (seleccionado = primary)
    val segColors = SegmentedButtonDefaults.colors(
        activeContainerColor = MaterialTheme.colorScheme.primary,
        activeContentColor   = MaterialTheme.colorScheme.onPrimary,
        inactiveContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
        inactiveContentColor   = MaterialTheme.colorScheme.onSurfaceVariant
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.checkout_confirm_title), color = MaterialTheme.colorScheme.primary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                Text(
                    stringResource(R.string.checkout_confirm_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                /* ── Selector modo: Recogida vs Envío ── */
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selected == OrderMode.PICKUP,
                        onClick = {
                            selected = OrderMode.PICKUP
                            onPickMode(OrderMode.PICKUP)
                        },
                        shape  = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        colors = segColors,
                        icon   = { Icon(Icons.Outlined.Store, contentDescription = null) },
                        label  = { Text(stringResource(R.string.checkout_mode_pickup)) }
                    )
                    SegmentedButton(
                        selected = selected == OrderMode.DELIVERY,
                        onClick = {
                            selected = OrderMode.DELIVERY
                            onPickMode(OrderMode.DELIVERY)
                        },
                        shape  = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        colors = segColors,
                        // aire extra para el icono respecto al separador central
                        icon   = {
                            Icon(
                                Icons.Outlined.DeliveryDining,
                                contentDescription = null,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        },
                        label  = { Text(stringResource(R.string.checkout_mode_delivery)) }
                    )
                }

                /* ── Bloque dirección (solo si DELIVERY) ── */
                if (selected == OrderMode.DELIVERY) {
                    if (addressId.isNullOrBlank()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.checkout_no_address), style = MaterialTheme.typography.bodyMedium)
                            AssistChip(
                                onClick = onAddAddress,
                                label = { Text(stringResource(R.string.checkout_add_address)) },
                                leadingIcon = { Icon(Icons.Outlined.AddLocationAlt, contentDescription = null) }
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                stringResource(R.string.checkout_deliver_to_label),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val cardShape = MaterialTheme.shapes.large
                            OutlinedCard(
                                shape = cardShape,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                                )
                            ) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.LocationOn, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        addressLabel ?: stringResource(R.string.checkout_loading_address),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    TextButton(onClick = onChangeAddress) { Text(stringResource(R.string.checkout_change_address)) }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val canProceed = when (selected) {
                OrderMode.PICKUP   -> true
                OrderMode.DELIVERY -> !addressId.isNullOrBlank()
                null               -> false
            }
            val confirmIcon = when (selected) {
                OrderMode.PICKUP   -> Icons.Outlined.Store
                OrderMode.DELIVERY -> Icons.Outlined.DeliveryDining
                null               -> null
            }
            Button(enabled = canProceed, onClick = onProceed) {
                confirmIcon?.let { Icon(it, contentDescription = null) }
                if (confirmIcon != null) Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.checkout_continue))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.checkout_close)) } }
    )
}
