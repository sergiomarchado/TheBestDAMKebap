package com.sergiom.thebestdamkebap.view.orders.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sergiom.thebestdamkebap.R
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
    val ctx = LocalContext.current

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
            } ?: stringResource(R.string.orders_date_pending)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.orders_card_title, o.id.takeLast(6)),
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
                Text(
                    pluralStringResource(
                        R.plurals.orders_items_count,
                        o.itemsCount,
                        o.itemsCount
                    ),
                    modifier = Modifier.weight(1f)
                )
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
                    label = {
                        Text(
                            if (expanded)
                                stringResource(R.string.orders_hide_details)
                            else
                                stringResource(R.string.orders_show_details)
                        )
                    }
                )
                AssistChip(
                    onClick = onRepeat,
                    label = { Text(stringResource(R.string.orders_repeat_order_cta)) }
                )
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                val details = remember(o.reorderLines, ctx) {
                    buildFriendlyDetails(
                        o.reorderLines
                    ) { id, args -> ctx.getString(id, *args) }
                }
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
