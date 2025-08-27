package com.sergiom.thebestdamkebap.view.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sergiom.thebestdamkebap.domain.order.OrderMode
import com.sergiom.thebestdamkebap.navigation.HomeRoutes
import com.sergiom.thebestdamkebap.view.address.AddressEditScreen
import com.sergiom.thebestdamkebap.view.address.AddressListScreen
import com.sergiom.thebestdamkebap.view.cart.CartScreen
import com.sergiom.thebestdamkebap.view.orders.OrdersScreen
import com.sergiom.thebestdamkebap.view.products.ProductsScreen
import com.sergiom.thebestdamkebap.view.profile.ProfileScreen
import com.sergiom.thebestdamkebap.view.settings.SettingsScreen
import com.sergiom.thebestdamkebap.view.home.start.HomeStartScreen
import com.sergiom.thebestdamkebap.view.home.start.OrderGate
import com.sergiom.thebestdamkebap.view.home.start.utils.formatAddressLine
import com.sergiom.thebestdamkebap.viewmodel.home.homestart.HomeStartViewModel
import com.sergiom.thebestdamkebap.viewmodel.order.OrderGateViewModel

/**
 * Grafo interno de **Home** (NavHost hijo).
 *
 * Responsabilidades:
 * - Define los destinos internos de Home (portada, productos, cuenta, direcciones, carrito, etc.).
 * - Aplica el "gate" de pedido (modo/dirección) antes de entrar en [ProductsScreen].
 *
 * Notas de integración:
 * - Este NavHost debe usar un **NavController hijo** (no el raíz) creado por el shell de Home
 *   (p. ej. HomeScreen), para que el back stack de Home sea independiente del grafo raíz.
 * - `isGuest` y los callbacks `onRequestLogin/Register` permiten a [OrderGate] redirigir a Auth
 *   cuando el invitado necesite autenticarse para completar el flujo.
 *
 * @param navController NavController **hijo** del flujo de Home.
 * @param isGuest Indica si el usuario actual es invitado.
 * @param onRequestLogin Callback para solicitar apertura del flujo de Login (lo atiende el grafo raíz).
 * @param onRequestRegister Callback para solicitar apertura del flujo de Register (lo atiende el grafo raíz).
 */
@Composable
fun HomeNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    isGuest: Boolean,
    onRequestLogin: () -> Unit,
    onRequestRegister: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoutes.HOME,
        modifier = modifier
    ) {
        /* ───────── Portada / Inicio ───────── */
        composable(HomeRoutes.HOME) {
            // VM que actualiza el contexto de pedido en un repositorio de sesión (OrderSessionRepository)
            val gateVm: OrderGateViewModel = hiltViewModel()

            HomeStartScreen(
                onStartOrder = { mode, addressId ->
                    // 1) Persistir el contexto de pedido seleccionado (modo + dirección).
                    // 2) Navegar a Productos. El gate validará que la sesión esté completa.
                    gateVm.confirmStart(mode.toDomain(), addressId)
                    navController.navigate(HomeRoutes.PRODUCTS) {
                        launchSingleTop = true
                    }
                },
                onAddAddress = { navController.navigate(HomeRoutes.AddressEdit.routeFor()) },
                onManageAddresses = { navController.navigate(HomeRoutes.ADDRESSES) }
            )
        }

        /* ───────── Ofertas ───────── */
        composable(HomeRoutes.OFFERS) { PlaceholderScreen("Ofertas") }

        /* ───────── Productos (protegido por gate) ─────────
         * Si falta modo/dirección, [OrderGate] muestra un BottomSheet para completarlos.
         */
        composable(HomeRoutes.PRODUCTS) {
            OrderGate(
                isGuest = isGuest,
                onReady = { ProductsScreen() },
                onAddAddress = { navController.navigate(HomeRoutes.AddressEdit.routeFor()) },
                onManageAddresses = { navController.navigate(HomeRoutes.ADDRESSES) },
                onRequestLogin = onRequestLogin,
                onRequestRegister = onRequestRegister
            )
        }

        /* ───────── Cuenta ───────── */
        composable(HomeRoutes.PROFILE)  { ProfileScreen() }
        composable(HomeRoutes.SETTINGS) { SettingsScreen() }
        composable(HomeRoutes.ORDERS)   { OrdersScreen(
            onOpenCart = {
                navController.navigate(HomeRoutes.CART) {
                    launchSingleTop = true
                    restoreState = true
                }
            }
        ) }

        /* ───────── Direcciones: listado ───────── */
        composable(HomeRoutes.ADDRESSES) {
            AddressListScreen(
                onBack = { navController.popBackStack() },
                onAddNew = { navController.navigate(HomeRoutes.AddressEdit.routeFor()) },
                onEdit = { aid -> navController.navigate(HomeRoutes.AddressEdit.routeFor(aid)) }
            )
        }

        /* ───────── Direcciones: crear/editar ─────────
         * Ruta patrón con argumento opcional en query (?aid={aid}). Si no se pasa, crea nueva.
         */
        composable(
            route = HomeRoutes.ADDRESS_EDIT,
            arguments = listOf(
                navArgument(HomeRoutes.AddressEdit.ARG_AID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val aid = backStackEntry.arguments?.getString(HomeRoutes.AddressEdit.ARG_AID)
            AddressEditScreen(
                aid = aid,
                onClose = { navController.popBackStack() }
            )
        }

        /* ───────── Carrito ─────────
         * Reutilizamos HomeStartViewModel SOLO para acceder a direcciones (ver sección "Revisión").
         */
        composable(HomeRoutes.CART) {
            val hsVm: HomeStartViewModel = hiltViewModel()
            val homeUi by hsVm.ui.collectAsStateWithLifecycle()

            // Lambda estable que mapea id → etiqueta de dirección (evita recrearla en cada recomposición)
            val addressLabelProvider: (String) -> String? = remember(homeUi.allAddresses) {
                { aid ->
                    homeUi.allAddresses
                        .firstOrNull { it.id == aid }
                        ?.let { formatAddressLine(it.street, it.number, it.city) }
                }
            }

            CartScreen(
                onBack = { navController.popBackStack() },
                onGoToOrders = { _ ->
                    navController.navigate(HomeRoutes.ORDERS) { launchSingleTop = true }
                },
                onAddAddress = { navController.navigate(HomeRoutes.AddressEdit.routeFor()) },
                onManageAddresses = { navController.navigate(HomeRoutes.ADDRESSES) },
                addressLabelProvider = addressLabelProvider
            )
        }
    }
}

/* ═══════════ Placeholder mínimo ═══════════ */
@Composable
private fun PlaceholderScreen(@Suppress("SameParameterValue") title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
    }
}

/* Helper: mapear el modo de Home VM al modo de dominio */
private fun HomeStartViewModel.Mode.toDomain(): OrderMode =
    when (this) {
        HomeStartViewModel.Mode.DELIVERY -> OrderMode.DELIVERY
        HomeStartViewModel.Mode.PICKUP   -> OrderMode.PICKUP
    }
