// com/sergiom/thebestdamkebap/view/cart/CartScreen.kt
package com.sergiom.thebestdamkebap.view.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.storage.FirebaseStorage
import com.sergiom.thebestdamkebap.core.imageloading.StorageImage
import com.sergiom.thebestdamkebap.domain.cart.CartItem
import com.sergiom.thebestdamkebap.domain.cart.MenuLine
import com.sergiom.thebestdamkebap.domain.cart.ProductLine
import com.sergiom.thebestdamkebap.domain.order.ProductCustomization
import com.sergiom.thebestdamkebap.viewmodel.cart.CartViewModel
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    viewModel: CartViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onCheckout: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nf = remember {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-ES")).apply {
            currency = Currency.getInstance("EUR") // opcional, por si el símbolo no fuese €
        }
    }
    val storage = remember { FirebaseStorage.getInstance() }

    // Resolver nombres de productId para los menús
    val allProductIds = remember(state.items) {
        buildSet {
            state.items.forEach { line ->
                when (line) {
                    is ProductLine -> add(line.productId)
                    is MenuLine -> line.selections.values.flatten().forEach { sel -> add(sel.productId) }
                }
            }
        }
    }
    val productMap by viewModel.productMap.collectAsStateWithLifecycle()
    LaunchedEffect(allProductIds) { viewModel.resolveProducts(allProductIds) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                title = { Text(text = "Tu carrito", color = MaterialTheme.colorScheme.primary) }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(), // un pelín de aire siempre
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = nf.format(state.totalCents / 100.0),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        enabled = state.items.isNotEmpty(),
                        onClick = onCheckout
                    ) { Text("Pagar") }
                }
            }
        }
    ) { padding ->
        if (state.items.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Tu carrito está vacío")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.items, key = { it.lineId }) { item ->
                    CartItemRow(
                        item = item,
                        productNameOf = { id -> productMap[id]?.name ?: id },
                        storage = storage,
                        nf = nf,
                        onInc = { viewModel.inc(item.lineId) },
                        onDec = { viewModel.dec(item.lineId) },
                        onRemove = { viewModel.remove(item.lineId) },
                    )
                }
                item { Spacer(Modifier.height(8.dp)) } // por si acaso frente al FAB
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    productNameOf: (String) -> String,     // resolver nombre de productId
    storage: FirebaseStorage,
    nf: NumberFormat,
    onInc: () -> Unit,
    onDec: () -> Unit,
    onRemove: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Miniatura (snapshot de imagePath)
            Thumbnail(imagePath = item.imagePath, storage = storage)

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, maxLines = 2)

                when (item) {
                    is ProductLine -> {
                        ProductCustomizationSummary(item.customization)
                    }
                    is MenuLine -> {
                        MenuSummary(
                            line = item,
                            productNameOf = productNameOf
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        nf.format(item.unitPriceCents / 100.0),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedIconButton(onClick = onDec) { Text("−") }
                    Text("${item.qty}", Modifier.padding(horizontal = 10.dp))
                    OutlinedIconButton(onClick = onInc) { Text("+") }
                    Spacer(Modifier.width(6.dp))
                    TextButton(onClick = onRemove) { Text("Eliminar") }
                }
            }
        }
    }
}

@Composable
private fun Thumbnail(imagePath: String?, storage: FirebaseStorage) {
    val boxModifier = Modifier
        .size(56.dp)
        .clip(RoundedCornerShape(12.dp))

    if (!imagePath.isNullOrBlank()) {
        val ref = remember(imagePath) { storage.reference.child(imagePath) }
        StorageImage(
            ref = ref,
            contentDescription = null,
            modifier = boxModifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(boxModifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

@Composable
private fun ProductCustomizationSummary(custom: ProductCustomization?) {
    if (custom?.removedIngredients?.isNotEmpty() == true) {
        Spacer(Modifier.height(4.dp))
        Text(
            "Sin: " + custom.removedIngredients.joinToString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MenuSummary(
    line: MenuLine,
    productNameOf: (String) -> String
) {
    var expanded by remember(line.lineId) { mutableStateOf(false) }

    // Resumen corto
    Text(
        if (!expanded) "Menú personalizado" else "Detalles del menú",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    IconButton(
        onClick = { expanded = !expanded },
        modifier = Modifier.size(28.dp)
    ) {
        Icon(
            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = if (expanded) "Ocultar" else "Ver detalles"
        )
    }

    if (expanded) {
        Spacer(Modifier.height(4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // Cada selección (no tenemos nombre del grupo aquí, listamos los productos)
            line.selections.values.forEach { list ->
                list.forEach { sel ->
                    val name = productNameOf(sel.productId)
                    val removed = sel.customization?.removedIngredients.orEmpty()
                    val extra = if (removed.isNotEmpty()) "  (Sin: ${removed.joinToString()})" else ""
                    Text(
                        "• $name$extra",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
