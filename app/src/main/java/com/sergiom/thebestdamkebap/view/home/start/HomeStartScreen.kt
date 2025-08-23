package com.sergiom.thebestdamkebap.view.home.start

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.LocalImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
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
    val ctaScale by animateFloatAsState(if (ctaEnabled) 1f else 0.98f, label = "ctaScale")

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (ui.loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

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
                    .graphicsLayer { scaleX = ctaScale; scaleY = ctaScale },
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

            // 4) Promos full-bleed (llamar con 'promos = …')
            if (ui.promos.isNotEmpty()) {
                PromoCarouselFullBleed(promos = ui.promos)
            }
        }
    }
}

/* ─────────── Subcomposables ─────────── */

@Composable
private fun ModeToggle(
    mode: HomeStartViewModel.Mode,
    enabled: Boolean,
    onChange: (HomeStartViewModel.Mode) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = mode == HomeStartViewModel.Mode.DELIVERY,
            onClick = { onChange(HomeStartViewModel.Mode.DELIVERY) },
            label = { Text("A domicilio") },
            enabled = enabled
        )
        FilterChip(
            selected = mode == HomeStartViewModel.Mode.PICKUP,
            onClick = { onChange(HomeStartViewModel.Mode.PICKUP) },
            label = { Text("Para recoger") },
            enabled = enabled
        )
    }
}

@Composable
private fun AddressBlock(
    addresses: List<Pair<String, String>>,
    selectedId: String?,
    enabled: Boolean,
    onSelect: (String) -> Unit,
    onAddNew: () -> Unit,
    onManage: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rotate by animateFloatAsState(if (expanded) 180f else 0f, label = "rotateExpand")

    Surface(tonalElevation = 4.dp, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null)
                Text(
                    text = when {
                        addresses.isEmpty() -> "Sin direcciones. Añade una nueva."
                        selectedId == null  -> "Elige una dirección"
                        else -> addresses.firstOrNull { it.first == selectedId }?.second ?: "Elige una dirección"
                    },
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = { expanded = !expanded }, enabled = enabled) {
                    Icon(
                        imageVector = Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.graphicsLayer { rotationZ = rotate }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onAddNew, enabled = enabled) { Text("Añadir nueva") }
                TextButton(onClick = onManage, enabled = enabled) { Text("Gestionar direcciones") }
            }
            AnimatedVisibility(visible = expanded && addresses.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    addresses.forEach { (id, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = id == selectedId,
                                onClick = { if (enabled) onSelect(id) },
                                enabled = enabled
                            )
                            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PromoCarouselFullBleed(
    promos: List<HomeStartViewModel.Promo>,
    autoAdvanceMillis: Long = 3200
) {
    val pagerState = rememberPagerState { promos.size }
    val colors = MaterialTheme.colorScheme
    val storage = remember { FirebaseStorage.getInstance() } // usa el bucket del google-services.json

    // Auto-slide
    LaunchedEffect(pagerState.pageCount) {
        if (pagerState.pageCount <= 1) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(autoAdvanceMillis)
            pagerState.animateScrollToPage((pagerState.currentPage + 1) % pagerState.pageCount)
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val promo = promos[page]
            val ref = remember(promo.storagePath) { storage.reference.child(promo.storagePath) }

            // Imagen full-bleed desde downloadUrl (funciona con Coil por defecto)
            StorageImage(
                ref = ref,
                contentDescription = promo.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Overlay (gradiente + título)
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                colors.primary.copy(alpha = 0f),
                                colors.primary.copy(alpha = 0.55f)
                            )
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Text(promo.title, style = MaterialTheme.typography.titleLarge, color = colors.onPrimary)
            }
        }

        // Dots
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(pagerState.pageCount) { i ->
                val active = i == pagerState.currentPage
                Box(
                    Modifier
                        .size(if (active) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (active) Color.White else Color.White.copy(alpha = 0.5f))
                )
            }
        }
    }
}

/** Carga la downloadUrl de un StorageReference y la muestra con Coil. */
@Composable
private fun StorageImage(
    ref: com.google.firebase.storage.StorageReference,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    // 1) Intento obtener la URL desde la caché
    val cached = remember(ref.path) { StorageUrlMemoryCache.get(ref.path) }

    // 2) Estado de la URL (null=cargando, ""=error, otra=ok)
    val urlState = produceState(initialValue = cached, ref.path) {
        if (cached != null) return@produceState  // nada que resolver
        ref.downloadUrl
            .addOnSuccessListener { url ->
                val u = url.toString()
                StorageUrlMemoryCache.put(ref.path, u)
                value = u
            }
            .addOnFailureListener {
                value = ""
            }
    }

    when (val u = urlState.value) {
        null -> Box(modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        ""   -> Box(modifier, contentAlignment = Alignment.Center) { Text("No cargó 😕") }
        else -> SubcomposeAsyncImage(
            model = u,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
            loading = { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() } },
            error   = {
                // Si la URL dejó de valer (p.ej. has reemplazado el fichero y cambió el token),
                // invalida la caché y vuelve a resolver en caliente.
                StorageUrlMemoryCache.invalidate(ref.path)
                // Forzamos un nuevo intento:
                // (resetearíamos a null para disparar de nuevo produceState)
            }
        )
    }
}


/* Utils */
private fun formatAddressLine(street: String, number: String, city: String): String =
    listOfNotNull(street.ifBlank { null }, number.ifBlank { null }, city.ifBlank { null })
        .joinToString(", ")

/* Cache simple en memoria para mapping path -> downloadUrl */
private object StorageUrlMemoryCache {
    private val map = java.util.concurrent.ConcurrentHashMap<String, String>()
    fun get(path: String): String? = map[path]
    fun put(path: String, url: String) { map[path] = url }
    fun invalidate(path: String) { map.remove(path) }
}

