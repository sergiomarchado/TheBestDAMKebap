package com.sergiom.thebestdamkebap.view.orders.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun StatusChip(status: String) {
    val color = when (status) {
        "PENDING"   -> MaterialTheme.colorScheme.secondaryContainer
        "READY"     -> MaterialTheme.colorScheme.tertiaryContainer
        "COMPLETED" -> MaterialTheme.colorScheme.primaryContainer
        "CANCELLED" -> MaterialTheme.colorScheme.errorContainer
        else        -> MaterialTheme.colorScheme.surfaceVariant
    }
    Surface(color = color, shape = MaterialTheme.shapes.small) {
        Text(
            status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

