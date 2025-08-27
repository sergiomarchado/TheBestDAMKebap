package com.sergiom.thebestdamkebap.view.cart.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sergiom.thebestdamkebap.domain.order.OrderMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConfirmShippingDialog(
    currentMode: OrderMode?,               // modo actual en la sesión
    addressId: String?,                    // id actual (si hay)
    addressLabel: String?,                 // etiqueta bonita (si la has resuelto)
    onDismiss: () -> Unit,
    onPickMode: (OrderMode) -> Unit,       // ⬅️ nuevo: fijar modo en VM
    onAddAddress: () -> Unit,              // navegar a crear/seleccionar
    onChangeAddress: () -> Unit,           // navegar a gestionar
    onProceed: () -> Unit                  // abrir el diálogo de pago
) {
    // estado local para feedback inmediato
    var selected by remember(currentMode) { mutableStateOf(currentMode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar tu pedido") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Elige cómo completar tu pedido y revisa la dirección si es envío a domicilio.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                /* ── Selector modo: Recogida vs Envío ── */
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = selected == OrderMode.PICKUP,
                        onClick = {
                            selected = OrderMode.PICKUP
                            onPickMode(OrderMode.PICKUP)   // ⬅️ fija en la sesión (limpia address)
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Store, // requiere material-icons-extended
                                contentDescription = null
                            )
                        },
                        label = { Text("Recogida") }
                    )
                    SegmentedButton(
                        selected = selected == OrderMode.DELIVERY,
                        onClick = {
                            selected = OrderMode.DELIVERY
                            onPickMode(OrderMode.DELIVERY) // ⬅️ fija en la sesión (mantiene address si había)
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.LocalShipping,
                                contentDescription = null
                            )
                        },
                        label = { Text("Envío a domicilio") }
                    )
                }

                /* ── Bloque dirección (solo si DELIVERY) ── */
                if (selected == OrderMode.DELIVERY) {
                    if (addressId.isNullOrBlank()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "No tienes una dirección seleccionada.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            AssistChip(
                                onClick = onAddAddress,
                                label = { Text("Añadir dirección") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.AddLocationAlt, contentDescription = null)
                                }
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Se entregará en:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            ElevatedCard {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.LocationOn, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        addressLabel ?: "Cargando dirección...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = onChangeAddress) { Text("Cambiar") }
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
            Button(
                enabled = canProceed,
                onClick = onProceed
            ) { Text("Continuar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}


