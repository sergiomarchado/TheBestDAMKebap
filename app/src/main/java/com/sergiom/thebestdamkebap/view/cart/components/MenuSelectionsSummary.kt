package com.sergiom.thebestdamkebap.view.cart.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sergiom.thebestdamkebap.domain.cart.MenuLine
import kotlin.collections.isNotEmpty
import kotlin.collections.joinToString
import kotlin.collections.orEmpty

/* ---------- Resumen de menús ---------- */
@Composable
internal fun MenuSelectionsSummary(
    line: MenuLine,
    productNameProvider: (suspend (String) -> String?)?
) {
    var expanded by remember(line.lineId) { mutableStateOf(false) }

    val ids = remember(line.lineId) {
        line.selections.values.flatten().map { it.productId }.distinct()
    }

    // Mapa id -> nombre “bonito”
    var names by remember(line.lineId) { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(productNameProvider, ids) {
        if (productNameProvider == null) return@LaunchedEffect
        // Si quieres performance extra: paraleliza con async/awaitAll
        val acc = mutableMapOf<String, String>()
        for (pid in ids) {
            val n = runCatching { productNameProvider(pid) }.getOrNull()
            if (!n.isNullOrBlank()) acc[pid] = n
        }
        if (acc.isNotEmpty()) names = acc
    }

    val label = if (expanded) "Ocultar detalles" else "Ver detalles"
    AssistChip(onClick = { expanded = !expanded }, label = { Text(label) })

    if (expanded) {
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            line.selections.values.flatten().forEach { sel ->
                val pretty = names[sel.productId] ?: sel.productId
                val removed = sel.customization?.removedIngredients.orEmpty()
                val suffix = if (removed.isNotEmpty()) " (sin ${removed.joinToString()})" else ""
                Text(
                    "• $pretty$suffix",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

