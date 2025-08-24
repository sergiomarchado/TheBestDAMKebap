package com.sergiom.thebestdamkebap.view.home.start

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergiom.thebestdamkebap.view.home.start.components.AddressBlock
import com.sergiom.thebestdamkebap.view.home.start.components.ModeToggle
import com.sergiom.thebestdamkebap.view.home.start.components.PromoCarouselFullBleed
import com.sergiom.thebestdamkebap.view.home.start.utils.formatAddressLine
import com.sergiom.thebestdamkebap.view.home.start.utils.neonGlow
import com.sergiom.thebestdamkebap.viewmodel.home.homestart.HomeStartViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeStartScreen(
    onStartOrder: (mode: HomeStartViewModel.Mode, addressId: String?) -> Unit,
    onAddAddress: () -> Unit,
    onManageAddresses: () -> Unit,
    viewModel: HomeStartViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val canInteract = !ui.loading

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { ev ->
            when (ev) {
                is HomeStartViewModel.Event.StartOrder -> onStartOrder(ev.mode, ev.addressId)
                is HomeStartViewModel.Event.Info       -> snackbar.showSnackbar(ev.text)
                is HomeStartViewModel.Event.Error      -> snackbar.showSnackbar(ev.text)
                HomeStartViewModel.Event.GoToAddAddress    -> onAddAddress()
                HomeStartViewModel.Event.GoToAddressesList -> onManageAddresses()
            }
        }
    }

    val addressItems by remember(ui.allAddresses) {
        derivedStateOf { ui.allAddresses.map { it.id to formatAddressLine(it.street, it.number, it.city) } }
    }

    val ctaEnabled = ui.canStart && canInteract
    // El botón "late" solo si se puede pulsar y no está cargando
    val attentionOn = ctaEnabled

// Escala base (ligeramente más pequeña si está deshabilitado)
    val baseScale by animateFloatAsState(if (ctaEnabled) 1f else 0.98f, label = "ctaBaseScale")

// Pulso infinito (1.0 -> 1.04 -> 1.0)
    val infinite = rememberInfiniteTransition(label = "ctaPulse")
    val pulse by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

// Escala final: base * pulso (solo si toca)
    val ctaScale = if (attentionOn) baseScale * pulse else baseScale

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (ui.loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            Text(
                text = "¡Bienvenid@!",
                modifier = Modifier.padding(start = 12.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Start,
            )
            Text(
                text = "Comienza a configurar tu pedido y échale un ojo a las novedades, por supuesto.",
                modifier = Modifier.padding(start = 12.dp, end = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
            )

            Box(Modifier.padding(horizontal = 16.dp)) {
                ModeToggle(
                    mode = ui.mode,
                    enabled = canInteract,
                    onChange = viewModel::onModeChange
                )
            }

            if (ui.mode == HomeStartViewModel.Mode.DELIVERY) {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    AddressBlock(
                        addresses = addressItems,
                        selectedId = ui.selectedAddressId,
                        enabled = canInteract,
                        onSelect = viewModel::onSelectAddress,
                        onAddNew = viewModel::onAddAddressClicked,
                        onManage = viewModel::onManageAddressesClicked
                    )
                }
            }

            FilledTonalButton(
                onClick = viewModel::onStartOrderClicked,
                enabled = ctaEnabled,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .graphicsLayer { scaleX = ctaScale; scaleY = ctaScale }
                    .neonGlow(
                        enabled = attentionOn,
                        color = MaterialTheme.colorScheme.secondary,
                        shape = MaterialTheme.shapes.large,   // mismo shape que el botón
                        thickness = 2.dp
                    ),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    when {
                        ui.loading -> "Cargando…"
                        ui.mode == HomeStartViewModel.Mode.DELIVERY -> "Empezar pedido a domicilio"
                        else -> "Empezar pedido para recoger"
                    }
                )
            }

            Text(
                text = "¡PROMOCIONES DEL MES!",
                modifier = Modifier.padding(start = 12.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Start,
            )
            Text(
                text = "Estas son algunas de las promociones que podrás disfrutar ahora mismo:",
                modifier = Modifier.padding(start = 12.dp, end = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
            )

            // 4) Promos full-bleed (llamar con 'promos = …')
            if (ui.promos.isNotEmpty()) {
                PromoCarouselFullBleed(promos = ui.promos)
            }
        }
    }
}