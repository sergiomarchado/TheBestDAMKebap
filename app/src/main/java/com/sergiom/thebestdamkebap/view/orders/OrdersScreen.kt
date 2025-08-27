// view/orders/OrdersScreen.kt
package com.sergiom.thebestdamkebap.view.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergiom.thebestdamkebap.domain.orders.OrderSummary
import com.sergiom.thebestdamkebap.domain.orders.ReorderLine
import com.sergiom.thebestdamkebap.viewmodel.orders.OrdersViewModel
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

@Composable
fun OrdersScreen(
    viewModel: OrdersViewModel = hiltViewModel(),
    onOpenCart: () -> Unit = {}   // llevar al carrito tras "Repetir pedido"
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()

    val nf = remember {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-ES")).apply {
            currency = Currency.getInstance("EUR")
        }
    }
    val fmt = remember {
        DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("es-ES"))
    }

    when {
        state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        state.isGuest -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Inicia sesión para ver tus pedidos.")
        }
        state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(state.error!!, color = MaterialTheme.colorScheme.error)
        }
        state.list.isEmpty() -> Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.Start
        ) {
            TitleBar()
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tienes pedidos todavía.")
            }
        }
        else -> {
            Column(Modifier.fillMaxSize()) {
                TitleBar()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.list, key = { it.id }) { o ->
                        OrderCard(
                            o = o,
                            nf = nf,
                            fmt = fmt,
                            onRepeat = {
                                viewModel.repeatOrder(o.reorderLines)
                                onOpenCart()
                            }
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun TitleBar() {
    Text(
        text = "Mis últimos pedidos",
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@Composable
private fun OrderCard(
    o: OrderSummary,
    nf: NumberFormat,
    fmt: DateTimeFormatter,
    onRepeat: () -> Unit
) {
    var expanded by remember(o.id) { mutableStateOf(false) }

    Card(
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
                    "Pedido ${o.id.takeLast(6)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                StatusChip(o.status)
            }

            Text(
                fecha,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${o.itemsCount} artículo(s)", modifier = Modifier.weight(1f))
                Text(nf.format(o.totalCents / 100.0), fontWeight = FontWeight.SemiBold)
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
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: String) {
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

/* ───────────────── helpers de formateo legible (sin claves internas) ───────────────── */

private fun buildFriendlyDetails(lines: List<ReorderLine>): List<String> = buildList {
    lines.forEach { line ->
        when (line) {
            is ReorderLine.Product -> {
                val base = line.name?.takeIf { it.isNotBlank() } ?: prettyNameFromId(line.productId)
                val extra = if (line.removedIngredients.isEmpty()) "" else " (sin ${line.removedIngredients.joinToString()})"
                add("${line.qty} × $base$extra")
            }
            is ReorderLine.Menu -> {
                val menuName = line.name?.takeIf { it.isNotBlank() } ?: prettyNameFromId(line.menuId)
                add("${line.qty} × $menuName")
                line.selections.forEach { (groupKey, list) ->
                    val label = groupLabelEs(groupKey)
                    val content = list.joinToString(", ") { sel ->
                        val base = prettyNameFromId(sel.productId)
                        if (sel.removedIngredients.isEmpty()) base
                        else "$base (sin ${sel.removedIngredients.joinToString()})"
                    }
                    add("   · $label: $content")
                }
            }
        }
    }
}

/** Mapea claves internas a etiquetas de UI en español. */
private fun groupLabelEs(key: String): String = when (key.lowercase()) {
    "main", "principal" -> "Principal"
    "side", "acompanamiento", "acompañamiento" -> "Acompañamiento"
    "drink", "bebida" -> "Bebida"
    else -> "Selección"
}

/** Convierte ids del tipo `cocacola-zero` → `Coca-Cola Zero`, `patatas-fritas-grandes` → `Patatas fritas grandes`. */
private fun prettyNameFromId(id: String): String {
    val base = id.replace('_', '-').replace('-', ' ').trim()
    var s = base.lowercase(Locale.forLanguageTag("es-ES"))

    // Normalizaciones conocidas
    s = s.replace("cocacola", "coca-cola")   // usa U+2011 no-break hyphen para verse mejor
    s = s.replace("nestea limon", "nestea limón")
    s = s.replace("kebap", "kebab") // por si acaso

    // Capitaliza cada palabra
    return s.split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { w ->
            w.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("es-ES")) else it.toString() }
        }
}
