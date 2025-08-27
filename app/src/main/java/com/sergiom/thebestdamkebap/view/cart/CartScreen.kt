package com.sergiom.thebestdamkebap.view.cart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergiom.thebestdamkebap.view.cart.components.CartItemRow
import com.sergiom.thebestdamkebap.view.cart.components.ConfirmShippingDialog
import com.sergiom.thebestdamkebap.view.cart.components.PaymentDialog
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
                title = { Text(text = "Tu Pedido:", color = MaterialTheme.colorScheme.primary) },
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

    // ─────────────────────────────────────────────────────────────────────────────
// Resultado OK
// ─────────────────────────────────────────────────────────────────────────────
    successOrderId?.let { oid ->
        AlertDialog(
            onDismissRequest = { successOrderId = null },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("¡Pedido realizado!") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Tu número de pedido es:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedCard(
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                        )
                    ) {
                        Text(
                            oid,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        "Guárdalo por si necesitas consultar el estado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    successOrderId = null
                    onGoToOrders(oid)
                }) { Text("Ver pedido") }
            },
            dismissButton = {
                TextButton(onClick = { successOrderId = null }) { Text("Cerrar") }
            }
        )
    }

// ─────────────────────────────────────────────────────────────────────────────
// Errores
// ─────────────────────────────────────────────────────────────────────────────
    errorMsg?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorMsg = null },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("No se pudo completar") },
            text  = { Text(msg) },
            confirmButton = {
                Button(onClick = { errorMsg = null }) { Text("Aceptar") }
            }
        )
    }
}