package com.sergiom.thebestdamkebap.view.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.sergiom.thebestdamkebap.viewmodel.cart.CartViewModel

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
            val gateVm: OrderGateViewModel = hiltViewModel()
            HomeStartScreen(
                onStartOrder = { mode, addressId ->
                    gateVm.confirmStart(mode.toDomain(), addressId)
                    navController.navigate(HomeRoutes.PRODUCTS) { launchSingleTop = true }
                },
                onAddAddress = { navController.navigate(HomeRoutes.AddressEdit.routeFor()) },
                onManageAddresses = { navController.navigate(HomeRoutes.ADDRESSES) }
            )
        }

        /* ───────── Ofertas ───────── */
        composable(HomeRoutes.OFFERS) { PlaceholderScreen("Ofertas") }

        /* ───────── Productos (protegido por gate) ───────── */
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
        composable(HomeRoutes.ORDERS)   {
            OrdersScreen(
                onOpenCart = {
                    navController.navigate(HomeRoutes.CART) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        /* ───────── Direcciones: listado ───────── */
        composable(HomeRoutes.ADDRESSES) {
            AddressListScreen(
                onBack = { navController.popBackStack() },
                onAddNew = { navController.navigate(HomeRoutes.AddressEdit.routeFor()) },
                onEdit = { aid -> navController.navigate(HomeRoutes.AddressEdit.routeFor(aid)) },
                onSelect = { aid ->
                    // devolver resultado y volver
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("selectedAddressId", aid)
                    navController.popBackStack()
                }
            )
        }

        /* ───────── Direcciones: crear/editar ───────── */
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
                onClose = { savedId ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("selectedAddressId", savedId)
                    navController.popBackStack()
                }
            )
        }

        /* ───────── Carrito ───────── */
        composable(HomeRoutes.CART) { backStackEntry ->
            // Etiquetas de direcciones para render
            val hsVm: HomeStartViewModel = hiltViewModel()
            val homeUi by hsVm.ui.collectAsStateWithLifecycle()
            val addressLabelProvider: (String) -> String? = remember(homeUi.allAddresses) {
                { aid ->
                    homeUi.allAddresses
                        .firstOrNull { it.id == aid }
                        ?.let { formatAddressLine(it.street, it.number, it.city) }
                }
            }

            // Recoger resultado desde Direcciones y fijarlo en la sesión
            val cartVm: CartViewModel = hiltViewModel()

            // ✅ Clave: usar el backStackEntry del propio composable como key de remember
            val cartEntry = remember(backStackEntry) { backStackEntry }
            val selectedAddrFlow = remember(cartEntry) {
                cartEntry.savedStateHandle.getStateFlow<String?>("selectedAddressId", null)
            }
            val selectedAddrId by selectedAddrFlow.collectAsStateWithLifecycle()

            LaunchedEffect(selectedAddrId) {
                selectedAddrId?.let { id ->
                    cartVm.setAddress(id)
                    // Limpia la clave en el mismo SavedStateHandle del entry de CART
                    cartEntry.savedStateHandle["selectedAddressId"] = null
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

/* Placeholder mínimo para pantallas pendientes. */
@Composable
private fun PlaceholderScreen(@Suppress("SameParameterValue") title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
    }
}

/* Helper: mapear el modo de Home VM al modo de dominio. */
private fun HomeStartViewModel.Mode.toDomain(): OrderMode =
    when (this) {
        HomeStartViewModel.Mode.DELIVERY -> OrderMode.DELIVERY
        HomeStartViewModel.Mode.PICKUP   -> OrderMode.PICKUP
    }
