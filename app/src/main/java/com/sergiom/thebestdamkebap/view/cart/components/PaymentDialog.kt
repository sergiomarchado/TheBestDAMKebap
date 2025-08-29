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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sergiom.thebestdamkebap.R

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
        title = { Text(text = stringResource(R.string.checkout_payment_title), color = MaterialTheme.colorScheme.primary) },
        text = {
            if (processing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Column {
                        Text(stringResource(R.string.checkout_payment_processing_title), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            stringResource(R.string.checkout_payment_processing_subtitle),
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
                                    stringResource(R.string.checkout_payment_amount_label),
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
                        stringResource(R.string.checkout_payment_disclaimer),
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
                Text(stringResource(R.string.checkout_payment_confirm_cta))
            }
        },
        dismissButton = {
            TextButton(enabled = !processing, onClick = onDismiss) {
                Text(stringResource(R.string.checkout_payment_cancel))
            }
        }
    )
}
