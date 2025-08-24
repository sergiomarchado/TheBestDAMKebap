// view/home/start/OrderGate.kt
package com.sergiom.thebestdamkebap.view.home.start

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergiom.thebestdamkebap.view.home.start.components.AddressBlock
import com.sergiom.thebestdamkebap.view.home.start.components.ModeToggle
import com.sergiom.thebestdamkebap.view.home.start.utils.formatAddressLine
import com.sergiom.thebestdamkebap.viewmodel.home.homestart.HomeStartViewModel
import com.sergiom.thebestdamkebap.viewmodel.order.OrderGateViewModel
import com.sergiom.thebestdamkebap.domain.order.OrderMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderGate(
    isGuest: Boolean,
    onReady: @Composable () -> Unit,
    onAddAddress: () -> Unit,
    onManageAddresses: () -> Unit,
    onRequestLogin: () -> Unit,
    onRequestRegister: () -> Unit,
    gateVm: OrderGateViewModel = hiltViewModel(),
    startVm: HomeStartViewModel = hiltViewModel()
) {
    val ctx by gateVm.context.collectAsStateWithLifecycle()
    val ui  by startVm.ui.collectAsStateWithLifecycle()

    // ── Regla de apertura del sheet ───────────────────────────────────────────────
    val mustGate: Boolean = if (isGuest) {
        // Invitado: solo pedimos si no hay contexto y no eligió "solo mirando"
        ctx.mode == null && !ctx.browsingOnly
    } else {
        // Logeado: exigimos configuración válida siempre
        val pickupOk   = ui.mode == HomeStartViewModel.Mode.PICKUP
        val deliveryOk = ui.mode == HomeStartViewModel.Mode.DELIVERY &&
                ui.allAddresses.isNotEmpty() && ui.selectedAddressId != null
        !(pickupOk || deliveryOk)
    }

    if (!mustGate) {
        onReady()
        return
    }

    ModalBottomSheet(
        onDismissRequest = { /* obligamos a elegir; no se cierra tocando fuera */ },
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Configura tu pedido",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 1) Modo
            ModeToggle(
                mode = ui.mode,
                enabled = !ui.loading,
                onChange = startVm::onModeChange
            )

            // 2) Dirección (solo delivery)
            if (ui.mode == HomeStartViewModel.Mode.DELIVERY) {
                AddressBlock(
                    addresses = ui.allAddresses.map {
                        it.id to formatAddressLine(it.street, it.number, it.city)
                    },
                    selectedId = ui.selectedAddressId,
                    enabled = !ui.loading,
                    onSelect = startVm::onSelectAddress,
                    onAddNew = {
                        if (isGuest) onRequestLogin() else onAddAddress()
                    },
                    onManage = {
                        if (isGuest) onRequestLogin() else onManageAddresses()
                    }
                )

                if (isGuest) {
                    // Cuña de login/registro si es invitado e intenta añadir direcciones
                    AssistChipRow(
                        onLogin = onRequestLogin,
                        onRegister = onRequestRegister
                    )
                }
            }

            // 3) Acciones
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = {
                        gateVm.confirmStart(ui.mode.toDomain(), ui.selectedAddressId)
                    },
                    enabled = ui.canStart && !ui.loading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = MaterialTheme.shapes.large
                ) { Text("Empezar pedido") }

                if (isGuest) {
                    TextButton(
                        onClick = { gateVm.chooseBrowsing() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Solo estoy mirando") }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AssistChipRow(
    onLogin: () -> Unit,
    onRegister: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(onClick = onLogin,  label = { Text("Iniciar sesión") })
        AssistChip(onClick = onRegister, label = { Text("Registrarse") })
    }
}

private fun HomeStartViewModel.Mode.toDomain(): OrderMode =
    when (this) {
        HomeStartViewModel.Mode.DELIVERY -> OrderMode.DELIVERY
        HomeStartViewModel.Mode.PICKUP   -> OrderMode.PICKUP
    }
