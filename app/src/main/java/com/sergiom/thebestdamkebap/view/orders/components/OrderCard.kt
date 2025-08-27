package com.sergiom.thebestdamkebap.view.orders.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sergiom.thebestdamkebap.domain.orders.OrderSummary
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun OrderCard(
    o: OrderSummary,
    nf: NumberFormat,
    fmt: DateTimeFormatter,
    onRepeat: () -> Unit
) {
    var expanded by remember(o.id) { mutableStateOf(false) }

    Card(
        modifier = Modifier.padding(bottom = 18.dp),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val fecha = o.createdAtMillis?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(fmt)
            } ?: "Pendiente de fecha…"

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Pedido ${o.id.takeLast(6)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.weight(1f))
                StatusChip(o.status)
            }
            Spacer(Modifier.height(4.dp))
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )

            Text(
                fecha,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${o.itemsCount} artículo(s)", modifier = Modifier.weight(1f))
                Text(
                    text = nf.format(o.totalCents / 100.0),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { expanded = !expanded },
                    label = { Text(if (expanded) "Ocultar detalle" else "Ver detalle") }
                )
                AssistChip(
                    onClick = onRepeat,
                    label = { Text("Repetir pedido") }
                )
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                val details = remember(o.reorderLines) { buildFriendlyDetails(o.reorderLines) }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    details.forEach { line ->
                        Text(
                            "• $line",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

