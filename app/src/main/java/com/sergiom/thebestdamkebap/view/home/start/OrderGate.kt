package com.sergiom.thebestdamkebap.view.home.start

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergiom.thebestdamkebap.domain.order.OrderMode
import com.sergiom.thebestdamkebap.view.home.start.components.AddressBlock
import com.sergiom.thebestdamkebap.view.home.start.components.ModeToggle
import com.sergiom.thebestdamkebap.view.home.start.utils.formatAddressLine
import com.sergiom.thebestdamkebap.viewmodel.home.homestart.HomeStartViewModel
import com.sergiom.thebestdamkebap.viewmodel.order.OrderGateViewModel
import com.sergiom.thebestdamkebap.R

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

    // Control local para “no volver a mostrar en esta sesión” (solo invitados)
    var dismissedThisSession by rememberSaveable { mutableStateOf(false) }

    // Reutilizamos HomeStartVM para conocer las direcciones actuales
    val startVm: HomeStartViewModel = hiltViewModel()
    val ui by startVm.ui.collectAsStateWithLifecycle()

    // ¿Existe la dirección guardada en la sesión?
    val addressExists by remember(ctx.addressId, ui.allAddresses) {
        derivedStateOf {
            val id = ctx.addressId
            id != null && ui.allAddresses.any { it.id == id }
        }
    }

    // Regla del gate:
    // - Invitado: mostrar hasta que pulse “Empezar pedido” o “Solo estoy mirando”.
    // - Registrado: permitir pasar si ctx.isActive y (PICKUP || DELIVERY con dirección válida), o browsingOnly.
    val needGate by remember(isGuest, dismissedThisSession, ctx, addressExists) {
        derivedStateOf {
            if (isGuest) {
                !dismissedThisSession
            } else {
                !ctx.browsingOnly && !(ctx.isActive && (ctx.mode != OrderMode.DELIVERY || addressExists))
            }
        }
    }

    // Estado del bottom sheet (sin colapsado parcial)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Mostrar/ocultar hoja siguiendo la necesidad del gate
    var showSheet by remember { mutableStateOf(needGate) }
    LaunchedEffect(needGate) { showSheet = needGate }

    // Cuando no se necesita gate → render del contenido real
    if (!showSheet) {
        onReady()
        return
    }

    // Bloquea el botón atrás mientras el gate es obligatorio (para que no se evite)
    @Suppress("KotlinConstantConditions")
    BackHandler(enabled = showSheet) { /* no-op: fuerza elegir */ }

    ModalBottomSheet(
        onDismissRequest = { /* obligado elegir o “mirar”; no cerramos tocando fuera */ },
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.gate_setup_title), style = MaterialTheme.typography.titleLarge)

            ModeToggle(
                mode = ui.mode,
                enabled = !ui.loading,
                onChange = startVm::onModeChange
            )

            if (ui.mode == HomeStartViewModel.Mode.DELIVERY) {
                AddressBlock(
                    addresses = ui.allAddresses.map { it.id to formatAddressLine(it.street, it.number, it.city) },
                    selectedId = ui.selectedAddressId,
                    enabled = !ui.loading,
                    onSelect = startVm::onSelectAddress,
                    onAddNew = onAddAddress,
                    onManage = onManageAddresses
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Usamos Button (filled) en lugar de FilledTonalButton si vas a colorearlo como primary
                Button(
                    onClick = {
                        gateVm.confirmStart(ui.mode.toDomain(), ui.selectedAddressId)
                        dismissedThisSession = true
                    },
                    enabled = ui.canStart && !ui.loading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) { Text(stringResource(R.string.gate_start_order_cta)) }

                TextButton(
                    onClick = {
                        gateVm.chooseBrowsing()
                        dismissedThisSession = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.gate_browsing_cta)) }
            }

            if (isGuest) {
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onRequestLogin, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.auth_sign_in))
                    }
                    OutlinedButton(onClick = onRequestRegister, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.auth_create_account))
                    }
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
