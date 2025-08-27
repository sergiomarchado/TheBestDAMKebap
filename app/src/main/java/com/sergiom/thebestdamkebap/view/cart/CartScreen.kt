package com.sergiom.thebestdamkebap.view.cart

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage
import com.sergiom.thebestdamkebap.core.imageloading.StorageImage
import com.sergiom.thebestdamkebap.domain.cart.CartItem
import com.sergiom.thebestdamkebap.domain.cart.MenuLine
import com.sergiom.thebestdamkebap.domain.cart.ProductLine
import com.sergiom.thebestdamkebap.view.cart.components.ConfirmShippingDialog
import com.sergiom.thebestdamkebap.viewmodel.cart.CartViewModel
import kotlinx.coroutines.flow.collectLatest
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Pantalla **Carrito**.
 *
 * - Muestra líneas del carrito y total.
 * - Orquesta confirmación de modo/dirección antes del pago.
 * - Simula el pago y, si es correcto, muestra el id de pedido y ofrece navegar a pedidos.
 *
 * Contratos:
 * - [addressLabelProvider]: opcional, resuelve etiqueta de una dirección (por id).
 * - [productNameProvider]: opcional, resuelve nombres “bonitos” para el desglose de menús.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    viewModel: CartViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onGoToOrders: (String) -> Unit = {},
    onAddAddress: () -> Unit = {},
    onManageAddresses: () -> Unit = {},
    addressLabelProvider: (suspend (String) -> String?)? = null,
    productNameProvider: (suspend (String) -> String?)? = null
) {
    val state   by viewModel.state.collectAsStateWithLifecycle()
    val placing by viewModel.placing.collectAsStateWithLifecycle()
    val ctx     by viewModel.orderCtx.collectAsStateWithLifecycle()

    // Formateo de moneda (EUR, locale ES)
    val nf = remember {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-ES")).apply {
            currency = Currency.getInstance("EUR")
        }
    }

    // Diálogos (saveable para no perderlos en rotaciones)
    var showConfirmShip by rememberSaveable { mutableStateOf(false) }
    var showPay by rememberSaveable { mutableStateOf(false) }
    var successOrderId by rememberSaveable { mutableStateOf<String?>(null) }
    var errorMsg by rememberSaveable { mutableStateOf<String?>(null) }

    // Etiqueta de dirección (lazily resuelta)
    var addressLabel by remember(ctx.addressId) { mutableStateOf<String?>(null) }
    LaunchedEffect(ctx.addressId, addressLabelProvider) {
        addressLabel = null
        val id = ctx.addressId
        if (!id.isNullOrBlank() && addressLabelProvider != null) {
            addressLabel = addressLabelProvider.invoke(id)
        }
    }

    // Eventos one-shot del VM
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { ev ->
            when (ev) {
                is CartViewModel.Event.Success -> {
                    showPay = false
                    showConfirmShip = false
                    successOrderId = ev.orderId
                }
                is CartViewModel.Event.Error -> errorMsg = ev.message
            }
        }
    }

    // Al abrir confirmación, intenta autocompletar dirección si falta
    LaunchedEffect(showConfirmShip) {
        if (showConfirmShip) viewModel.reconcileAddressIfNeeded()
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("Tu carrito") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = nf.format(state.totalCents / 100.0),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        enabled = state.items.isNotEmpty() && !placing,
                        onClick = {
                            when {
                                ctx.browsingOnly -> {
                                    errorMsg = "Estás en modo exploración. Selecciona el modo de pedido para continuar."
                                }
                                ctx.mode == null -> {
                                    errorMsg = "Selecciona un modo de pedido (Recogida o Envío a domicilio) antes de pagar."
                                }
                                else -> {
                                    viewModel.reconcileAddressIfNeeded()
                                    showConfirmShip = true
                                }
                            }
                        }
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.items, key = { it.lineId }) { item ->
                    CartItemRow(
                        item = item,
                        onInc = { viewModel.inc(item.lineId) },
                        onDec = { viewModel.dec(item.lineId) },
                        onRemove = { viewModel.remove(item.lineId) },
                        nf = nf,
                        productNameProvider = productNameProvider
                    )
                }
                item { Spacer(Modifier.height(96.dp)) } // margen para FAB, si lo hubiese
            }
        }
    }

    // Confirmación de modo/dirección
    if (showConfirmShip) {
        ConfirmShippingDialog(
            currentMode     = ctx.mode,
            addressId       = ctx.addressId,
            addressLabel    = addressLabel,
            onDismiss       = { showConfirmShip = false },
            onPickMode      = { mode -> viewModel.chooseMode(mode) },
            onAddAddress    = {
                showConfirmShip = false
                onAddAddress()
            },
            onChangeAddress = {
                showConfirmShip = false
                onManageAddresses()
            },
            onProceed       = {
                showConfirmShip = false
                showPay = true
            }
        )
    }

    // Pago simulado
    if (showPay) {
        PaymentDialog(
            amountLabel = nf.format(state.totalCents / 100.0),
            processing = placing,
            onDismiss = { if (!placing) showPay = false },
            onConfirm = { viewModel.checkout() }
        )
    }

    // Resultado OK
    successOrderId?.let { oid ->
        AlertDialog(
            onDismissRequest = { successOrderId = null },
            title = { Text("¡Pedido realizado!") },
            text  = { Text("Tu número de pedido es:\n$oid") },
            confirmButton = {
                TextButton(onClick = {
                    successOrderId = null
                    onGoToOrders(oid)
                }) { Text("Ver pedido") }
            },
            dismissButton = {
                TextButton(onClick = { successOrderId = null }) { Text("Cerrar") }
            }
        )
    }

    // Errores
    errorMsg?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorMsg = null },
            title = { Text("No se pudo completar") },
            text  = { Text(msg) },
            confirmButton = { TextButton(onClick = { errorMsg = null }) { Text("Aceptar") } }
        )
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onInc: () -> Unit,
    onDec: () -> Unit,
    onRemove: () -> Unit,
    nf: NumberFormat,
    productNameProvider: (suspend (String) -> String?)?
) {
    // Firebase Storage (memoizado por bucket e imagen)
    val bucket = remember { FirebaseApp.getInstance().options.storageBucket }
    val storage = remember(bucket) {
        bucket?.takeIf { it.isNotBlank() }?.let { FirebaseStorage.getInstance("gs://$it") }
            ?: FirebaseStorage.getInstance()
    }

    ElevatedCard {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!item.imagePath.isNullOrBlank()) {
                    val ref = remember(item.imagePath) { storage.reference.child(item.imagePath!!) }
                    StorageImage(
                        ref = ref,
                        contentDescription = item.name,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    when (item) {
                        is ProductLine -> {
                            if (item.customization?.removedIngredients?.isNotEmpty() == true) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Sin: " + item.customization.removedIngredients.joinToString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        is MenuLine -> {
                            Spacer(Modifier.height(4.dp))
                            MenuSelectionsSummary(
                                line = item,
                                productNameProvider = productNameProvider
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    nf.format(item.unitPriceCents / 100.0),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(onClick = onDec, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("−") }
                Text("${item.qty}", Modifier.padding(horizontal = 8.dp))
                OutlinedButton(onClick = onInc, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("+") }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onRemove) { Text("Eliminar") }
            }
        }
    }
}

/* ---------- Resumen de menús ---------- */
@Composable
private fun MenuSelectionsSummary(
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

/* ---------- Pago simulado ---------- */
@Composable
private fun PaymentDialog(
    amountLabel: String,
    processing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Simular pago") },
        text = {
            if (processing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Procesando pago…")
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Importe a cobrar: $amountLabel")
                    Text("No se realiza ningún cargo real. Pulsa \"Pagar ahora\" para continuar.")
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !processing, onClick = onConfirm) { Text("Pagar ahora") }
        },
        dismissButton = {
            TextButton(enabled = !processing, onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
