package com.sergiom.thebestdamkebap.view.products.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilterChipDefaults.filterChipBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.storage.FirebaseStorage
import com.sergiom.thebestdamkebap.core.imageloading.StorageImage
import com.sergiom.thebestdamkebap.domain.catalog.Product
import com.sergiom.thebestdamkebap.domain.menu.Menu
import com.sergiom.thebestdamkebap.domain.menu.MenuAllowed
import com.sergiom.thebestdamkebap.domain.menu.MenuGroup
import com.sergiom.thebestdamkebap.domain.order.OrderMode
import com.sergiom.thebestdamkebap.domain.order.ProductCustomization
import com.sergiom.thebestdamkebap.view.products.components.products.utils.toPriceLabel

data class MenuSelection(
    val menuId: String,
    val selections: Map<String, List<Selection>>,
) {
    data class Selection(
        val productId: String,
        val customization: ProductCustomization? = null,
    )
}

@Composable
fun MenuBuilderDialog(
    menu: Menu,
    mode: OrderMode?,
    storage: FirebaseStorage,
    loadProductsByIds: suspend (List<String>) -> List<Product>,
    onDismiss: () -> Unit,
    onConfirm: (MenuSelection) -> Unit,
) {
    // ---- cargar productos permitidos (para nombres/ingredientes) ----
    val allIds =
        remember(menu.id) { menu.groups.flatMap {
            it.allowed.map { a -> a.productId } }
            .distinct()}
    var products by remember(menu.id) {
        mutableStateOf<Map<String, Product>>(emptyMap())
    }
    LaunchedEffect(allIds) {
        products =
            if (allIds.isEmpty()) emptyMap() else loadProductsByIds(allIds).associateBy { it.id }
    }

    // ---- selección por grupo (defaults) ----
    var selections by remember(menu.id) {
        mutableStateOf(
            menu.groups.associate { g ->
                val defaults =
                    g.allowed.filter { it.default }.map {
                        MenuSelection.Selection(it.productId)
                    }
                g.id to defaults
            }
        )
    }

    // abrir personalización de un producto concreto
    var customizing: Pair<String, String>? by remember { mutableStateOf(null) }

    val base = when (mode) {
        OrderMode.DELIVERY -> menu.prices.delivery ?: menu.prices.pickup
        OrderMode.PICKUP, null -> menu.prices.pickup ?: menu.prices.delivery
    } ?: 0L

    fun delta(a: MenuAllowed) = when (mode) {
        OrderMode.DELIVERY -> a.delta?.delivery ?: 0L
        OrderMode.PICKUP, null -> a.delta?.pickup ?: 0L
    }

    val totalCents = remember(selections, mode) {
        base + menu.groups.sumOf { g ->
            selections[g.id].orEmpty().sumOf { sel ->
                g.allowed.firstOrNull { it.productId == sel.productId }?.let { delta(it) } ?: 0L
            }
        }
    }
    val totalLabel = totalCents.toPriceLabel()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // —— ancho responsive del diálogo —— //
        BoxWithConstraints {
            val widthModifier =
                if (maxWidth < 720.dp) {
                    Modifier.fillMaxWidth(0.98f)
                } else {
                    Modifier
                        .fillMaxWidth(0.92f)
                        .widthIn(min = 720.dp, max = 1080.dp)
                }

            Surface(
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 6.dp,
                modifier = widthModifier
                    .padding(horizontal = 8.dp, vertical = 12.dp)
            ) {
                Column(Modifier.fillMaxWidth()) {

                    // ---------- HERO con imagen + scrim primary ----------
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(148.dp)
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    ) {
                        val path = menu.imagePath
                        if (!path.isNullOrBlank()) {
                            val ref = remember(path) {
                                storage.reference.child(path)
                            }
                            StorageImage(
                                ref = ref,
                                contentDescription = menu.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }

                        // scrim en primary para dar marca y legibilidad
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        0f to MaterialTheme
                                            .colorScheme.primary.copy(alpha = 0.85f),
                                        0.6f to MaterialTheme
                                            .colorScheme.primary.copy(alpha = 0.55f),
                                        1f to Color.Transparent
                                    )
                                )
                        )

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 4.dp, top = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                // === TÍTULO CON CONTORNO (stroke simulado) ===
                                OutlinedText(
                                    text = menu.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fill = MaterialTheme.colorScheme.onPrimary,
                                    outline = Color.White,
                                    stroke = 1.25.dp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Cerrar",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    // ---------- CONTENIDO ----------
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!menu.description.isNullOrBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = menu.description,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        menu.groups.forEach { group ->
                            GroupCard(
                                group = group,
                                products = products,
                                current = selections[group.id].orEmpty(),
                                onPick = { productId ->
                                    val curr =
                                        selections[group.id].orEmpty()

                                    val max = group.max.coerceAtLeast(1)

                                    val next = if (max == 1) {
                                        listOf(MenuSelection.Selection(productId))
                                    } else {
                                        if (curr.any { it.productId == productId }) {
                                            curr.filterNot { it.productId == productId }
                                        } else {
                                            if (curr.size >= max) curr
                                            else curr + MenuSelection.Selection(productId)
                                        }
                                    }
                                    selections = selections.toMutableMap().apply {
                                        put(group.id, next)
                                    }
                                },
                                onCustomize = { productId ->
                                    customizing = group.id to productId
                                }
                            )
                        }
                    }

                    HorizontalDivider(
                        Modifier, DividerDefaults.Thickness, DividerDefaults.color
                    )

                    // ---------- FOOTER ----------
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            totalLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    val enabled = remember(selections) {
                        menu.groups.all { g ->
                            val count = selections[g.id].orEmpty().size
                            count in g.min..g.max
                        }
                    }
                    Button(
                        onClick = { onConfirm(MenuSelection(menu.id, selections)) },
                        enabled = enabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .height(48.dp)
                    ) { Text("Añadir menú") }
                }
            }
        }
    }

    // ---- diálogo de personalización de producto dentro del menú ----
    customizing?.let { (groupId, productId) ->
        val p = products[productId]
        if (p != null) {
            ProductCustomizeDialog(
                product = p,
                mode = mode,
                onDismiss = { customizing = null },
                onConfirm = { custom ->
                    val list = selections[groupId].orEmpty()
                    val updated = list.map {
                        if (it.productId == productId) it.copy(customization = custom) else it
                    }
                    selections = selections.toMutableMap().apply { put(groupId, updated) }
                    customizing = null
                }
            )
        }
    }
}

@Composable
private fun GroupCard(
    group: MenuGroup,
    products: Map<String, Product>,
    current: List<MenuSelection.Selection>,
    onPick: (String) -> Unit,
    onCustomize: (String) -> Unit,
) {
    ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier
            .fillMaxWidth()
            .padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${current.size}/${group.max}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(group.allowed, key = { it.productId }) { allowed ->
                    val p = products[allowed.productId]
                    val selected = current.any { it.productId == allowed.productId }

                    FilterChip(
                        selected = selected,
                        onClick = { onPick(allowed.productId) },
                        shape = RoundedCornerShape(22.dp),
                        label = {
                            Text(
                                text = p?.name ?: allowed.productId,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .defaultMinSize(minWidth = 48.dp),
                        // 👇 badge “Personalizar” discreto, sobre fondo oscuro, solo cuando procede
                        trailingIcon = if (
                            selected && allowed.allowIngredientRemoval
                            && (p?.ingredients?.isNotEmpty() == true)
                        ) {
                            {
                                val noRipple = remember {
                                    MutableInteractionSource()
                                }
                                Box(
                                    modifier = Modifier
                                        .height(40.dp)
                                        .defaultMinSize(minWidth = 40.dp)  // ancho mínimo del badge
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(24.dp)
                                        )
                                        .clickable(              // abre personalización
                                            interactionSource = noRipple,
                                            indication = null
                                        ) { onCustomize(allowed.productId) }
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Personalizar",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            // ✅ seleccionado en PRIMARY (como querías)
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        border = filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                            enabled = true,
                            selected = selected
                        )
                    )
                }
            }

            if (current.size < group.min) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Selecciona al menos ${group.min}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/* -------------------- helper: texto con contorno simulado -------------------- */
@Composable
private fun OutlinedText(
    text: String,
    style: TextStyle,
    fill: Color,
    outline: Color,
    stroke: Dp = 1.dp,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    // Dibuja 8 copias desplazadas (N, S, E, O y diagonales) como "borde"
    val s = stroke
    Box {
        // contorno
        Text(
            text,
            style = style,
            color = outline,
            maxLines = maxLines,
            overflow = overflow,
            modifier = Modifier.offset(x = -s, y = -s)
        )
        Text(
            text,
            style = style,
            color = outline,
            maxLines = maxLines,
            overflow = overflow,
            modifier = Modifier.offset(x = s, y = -s)
        )
        Text(
            text,
            style = style,
            color = outline,
            maxLines = maxLines,
            overflow = overflow,
            modifier = Modifier.offset(x = -s, y = s)
        )
        Text(
            text,
            style = style,
            color = outline,
            maxLines = maxLines,
            overflow = overflow,
            modifier = Modifier.offset(x = s, y = s)
        )
        Text(
            text,
            style = style,
            color = outline,
            maxLines = maxLines,
            overflow = overflow,
            modifier = Modifier.offset(x = 0.dp, y = -s)
        )
        Text(
            text,
            style = style,
            color = outline,
            maxLines = maxLines,
            overflow = overflow,
            modifier = Modifier.offset(x = 0.dp, y = s)
        )
        Text(
            text,
            style = style,
            color = outline,
            maxLines = maxLines,
            overflow = overflow,
            modifier = Modifier.offset(x = -s, y = 0.dp)
        )
        Text(
            text,
            style = style,
            color = outline,
            maxLines = maxLines,
            overflow = overflow,
            modifier = Modifier.offset(x = s, y = 0.dp)
        )
        // relleno
        Text(text, style = style, color = fill, maxLines = maxLines, overflow = overflow)
    }
}
