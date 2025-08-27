package com.sergiom.thebestdamkebap.view.cart.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/* ---------- Pago simulado ---------- */
@Composable
internal fun PaymentDialog(
    amountLabel: String,
    processing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Simular pago", color = MaterialTheme.colorScheme.primary) },
        text = {
            if (processing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Column {
                        Text("Procesando pago…", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Esto puede tardar unos segundos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Ficha del importe
                    OutlinedCard(
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.CreditCard, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Importe a cobrar",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    amountLabel,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                    }

                    Text(
                        "No se realiza ningún cargo real. Pulsa \"Pagar ahora\" para continuar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !processing,
                onClick = onConfirm
            ) {
                Icon(Icons.Outlined.CreditCard, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Pagar ahora")
            }
        },
        dismissButton = {
            TextButton(enabled = !processing, onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
