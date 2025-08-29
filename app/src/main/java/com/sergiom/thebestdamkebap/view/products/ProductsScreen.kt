package com.sergiom.thebestdamkebap.view.products

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.Coil
import coil.ImageLoader
import coil.request.ImageRequest
import com.google.firebase.storage.FirebaseStorage
import com.sergiom.thebestdamkebap.core.firebase.rememberStorage
import com.sergiom.thebestdamkebap.core.imageloading.StorageImage
import com.sergiom.thebestdamkebap.domain.menu.Menu
import com.sergiom.thebestdamkebap.domain.order.OrderMode
import com.sergiom.thebestdamkebap.view.products.components.dialogs.MenuBuilderDialog
import com.sergiom.thebestdamkebap.view.products.components.dialogs.ProductCustomizeDialog
import com.sergiom.thebestdamkebap.view.products.components.products.CategoryRow
import com.sergiom.thebestdamkebap.view.products.components.products.ProductCard
import com.sergiom.thebestdamkebap.view.products.components.products.utils.toPriceLabel
import com.sergiom.thebestdamkebap.viewmodel.products.ProductsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.sergiom.thebestdamkebap.R

@Composable
fun ProductsScreen(
    viewModel: ProductsViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val storage = rememberStorage()
    val context = LocalContext.current

    // Prefetch: al cambiar la lista, precargamos las primeras N imágenes para mejorar la percepción de velocidad.
    LaunchedEffect(ui.items) {
        val paths = ui.items.mapNotNull { it.imagePath }.take(6)
        prefetchStorageImages(context, storage, paths)
    }

    // Estados locales para abrir los diálogos
    var productToCustomize by remember { mutableStateOf<com.sergiom.thebestdamkebap.domain.catalog.Product?>(null) }
    var menuToBuild by remember { mutableStateOf<Menu?>(null) }

    Column(Modifier.fillMaxSize()) {
        if (ui.loading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
            )
        }

        CategoryRow(
            categories = ui.categories,
            selectedId = ui.selectedCategoryId,
            onSelect = viewModel::onSelectCategory
        )
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        when {
            !ui.loading && ui.items.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.productsscreen_no_products))
                }
            }
            else -> {
                ItemsList(
                    items = ui.items,
                    mode = ui.mode,
                    storage = storage,
                    // Al tocar un producto abrimos su diálogo de personalización
                    onProductClick = { p -> productToCustomize = p },
                    // Al tocar un menú abrimos el configurador
                    onMenuClick = { m -> menuToBuild = m }
                )
            }
        }
    }

    // ---- DIALOG: personalización de producto ----
    val currentMode = ui.mode
    productToCustomize?.let { product ->
        ProductCustomizeDialog(
            product = product,
            mode = currentMode,
            onDismiss = { productToCustomize = null },
            onConfirm = { customization ->
                viewModel.addProductToCart(product, customization)
                productToCustomize = null
            }
        )
    }

    // ---- DIALOG: constructor de menús ----
    menuToBuild?.let { menu ->
        MenuBuilderDialog(
            menu = menu,
            mode = currentMode,
            storage = storage,
            loadProductsByIds = viewModel::loadProductsByIds,
            onDismiss = { menuToBuild = null },
            onConfirm = { selection ->
                viewModel.addMenuToCart(menu, selection)
                menuToBuild = null
            }
        )
    }
}

/* Lista unificada (productos y menús) */
@Composable
private fun ItemsList(
    items: List<ProductsViewModel.CatalogItem>,
    mode: OrderMode?,
    storage: FirebaseStorage,
    onProductClick: (com.sergiom.thebestdamkebap.domain.catalog.Product) -> Unit,
    onMenuClick: (Menu) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { "${it.kind}-${it.id}" }) { item ->
            when (item) {
                is ProductsViewModel.CatalogItem.ProductItem -> {
                    ProductCard(
                        product = item.product,
                        mode = mode,
                        storage = storage,
                        onClick = { onProductClick(item.product) }
                    )
                }
                is ProductsViewModel.CatalogItem.MenuItem -> {
                    MenuCard(
                        menu = item.menu,
                        mode = mode,
                        storage = storage,
                        onClick = { onMenuClick(item.menu) }
                    )
                }
            }
        }
    }
}

/* Tarjeta simple para Menú (precio base por modo) */
@Composable
private fun MenuCard(
    menu: Menu,
    mode: OrderMode?,
    storage: FirebaseStorage,
    onClick: (() -> Unit)? = null
) {
    val priceCents = when (mode) {
        OrderMode.DELIVERY -> menu.prices.delivery ?: menu.prices.pickup
        OrderMode.PICKUP, null -> menu.prices.pickup ?: menu.prices.delivery
    }
    val priceLabel = priceCents.toPriceLabel()

    val shape = MaterialTheme.shapes.large
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    val imageShape = RoundedCornerShape(
        topStart = shape.topStart,
        bottomStart = shape.bottomStart,
        topEnd = CornerSize(0),
        bottomEnd = CornerSize(0)
    )

    val content: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth()) {
            // Imagen
            Box(
                Modifier
                    .size(96.dp)
                    .align(Alignment.CenterVertically)
            ) {
                val path = menu.imagePath
                if (!path.isNullOrBlank()) {
                    val ref = remember(path) { storage.reference.child(path) }
                    StorageImage(
                        ref = ref,
                        contentDescription = menu.name, // Si es decorativa, pasa null
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(imageShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Placeholder simple; puedes mover el texto a strings.xml cuando quieras
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("IMG") }
                }
            }

            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(menu.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                if (!menu.description.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(menu.description, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
                Spacer(Modifier.height(6.dp))
                // Precio a la derecha
                Box(Modifier.fillMaxWidth()) {
                    Text(
                        priceLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = border,
            modifier = Modifier.fillMaxWidth()
        ) { content() }
    } else {
        Card(
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = border,
            modifier = Modifier.fillMaxWidth()
        ) { content() }
    }
}

/* ============================================================
 * Helper: prefetch de imágenes de Firebase Storage (barato y optativo).
 * - Resuelve downloadUrl por cada path y se lo pasa al ImageLoader de Coil.
 * - Se hace en IO y se ignoran errores (no bloquea la UI).
 * - Llamado desde LaunchedEffect(ui.items) con las primeras N rutas.
 * ============================================================ */
private suspend fun prefetchStorageImages(
    context: Context,
    storage: FirebaseStorage,
    paths: List<String>
) = withContext(Dispatchers.IO) {
    if (paths.isEmpty()) return@withContext
    val loader: ImageLoader = Coil.imageLoader(context)
    for (path in paths) {
        val url = try {
            storage.reference.child(path).downloadUrl.await().toString()
        } catch (_: Throwable) {
            null
        }
        if (url != null) {
            loader.enqueue(
                ImageRequest.Builder(context)
                    .data(url)
                    .build()
            )
        }
    }
}
