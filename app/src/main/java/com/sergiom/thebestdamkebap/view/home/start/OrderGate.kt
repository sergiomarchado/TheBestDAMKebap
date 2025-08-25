// view/home/start/OrderGate.kt
package com.sergiom.thebestdamkebap.view.home.start

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergiom.thebestdamkebap.domain.order.OrderMode
import com.sergiom.thebestdamkebap.view.home.start.components.AddressBlock
import com.sergiom.thebestdamkebap.view.home.start.components.ModeToggle
import com.sergiom.thebestdamkebap.view.home.start.utils.formatAddressLine
import com.sergiom.thebestdamkebap.viewmodel.home.homestart.HomeStartViewModel
import com.sergiom.thebestdamkebap.viewmodel.order.OrderGateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderGate(
    isGuest: Boolean,
    onReady: @Composable () -> Unit,
    onAddAddress: () -> Unit,
    onManageAddresses: () -> Unit,
    onRequestLogin: () -> Unit,
    onRequestRegister: () -> Unit,
    gateVm: OrderGateViewModel = hiltViewModel()
) {
    val ctx by gateVm.context.collectAsStateWithLifecycle()

    // Si es invitado: pedimos SIEMPRE al entrar la primera vez en esta sesión.
    var dismissedThisSession by rememberSaveable { mutableStateOf(false) }

    val needsSetup = if (isGuest) {
        !dismissedThisSession
    } else {
        ctx.mode == null && !ctx.browsingOnly
    }

    var showSheet by remember { mutableStateOf(needsSetup) }

    LaunchedEffect(isGuest, ctx.mode, ctx.browsingOnly, dismissedThisSession) {
        showSheet = if (isGuest) !dismissedThisSession else (ctx.mode == null && !ctx.browsingOnly)
    }

    if (!showSheet) {
        onReady()
        return
    }

    // Reutilizamos el VM de HomeStart para la UI de selección
    val startVm: HomeStartViewModel = hiltViewModel()
    val ui by startVm.ui.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = { /* obligado elegir o “mirar”; no cerramos tocando fuera */ },
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
                    onAddNew = onAddAddress,
                    onManage = onManageAddresses
                )
            }

            // 3) Acciones
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = {
                        gateVm.confirmStart(ui.mode.toDomain(), ui.selectedAddressId)
                        dismissedThisSession = true
                    },
                    enabled = ui.canStart && !ui.loading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = MaterialTheme.shapes.large
                ) { Text("Empezar pedido") }

                TextButton(
                    onClick = {
                        gateVm.chooseBrowsing()
                        dismissedThisSession = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Solo estoy mirando") }
            }

            // 4) Invitado: CTA de acceso/registro
            if (isGuest) {
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onRequestLogin,
                        modifier = Modifier.weight(1f)
                    ) { Text("Iniciar sesión") }
                    OutlinedButton(
                        onClick = onRequestRegister,
                        modifier = Modifier.weight(1f)
                    ) { Text("Registrarse") }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun HomeStartViewModel.Mode.toDomain(): OrderMode =
    when (this) {
        HomeStartViewModel.Mode.DELIVERY -> OrderMode.DELIVERY
        HomeStartViewModel.Mode.PICKUP   -> OrderMode.PICKUP
    }
