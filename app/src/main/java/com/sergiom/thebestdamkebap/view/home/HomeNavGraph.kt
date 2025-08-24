package com.sergiom.thebestdamkebap.view.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sergiom.thebestdamkebap.domain.order.OrderMode
import com.sergiom.thebestdamkebap.navigation.HomeRoutes
import com.sergiom.thebestdamkebap.view.home.screens.AddressEditScreen
import com.sergiom.thebestdamkebap.view.home.screens.AddressListScreen
import com.sergiom.thebestdamkebap.view.home.screens.OrdersScreen
import com.sergiom.thebestdamkebap.view.home.screens.ProfileScreen
import com.sergiom.thebestdamkebap.view.home.screens.SettingsScreen
import com.sergiom.thebestdamkebap.view.home.start.HomeStartScreen
import com.sergiom.thebestdamkebap.view.home.start.OrderGate
import com.sergiom.thebestdamkebap.viewmodel.home.homestart.HomeStartViewModel
import com.sergiom.thebestdamkebap.viewmodel.order.OrderGateViewModel

/**
 * Grafo interno de Home.
 *
 * - HOME: portada con selección (modo/dirección) y CTA "Empezar pedido".
 * - PRODUCTS: protegido por OrderGate (si faltan datos, muestra sheet).
 * - Otros: ofertas, perfil, direcciones, pedidos, ajustes.
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
            // VM que escribe en OrderSessionRepository (Hilt)
            val gateVm: OrderGateViewModel = hiltViewModel()

            HomeStartScreen(
                onStartOrder = { mode, addressId ->
                    // Guardar el contexto y navegar a Productos
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

        /* ───────── Productos (con gate) ─────────
         * Si falta modo/dirección, OrderGate abre un BottomSheet pidiendo esos datos.
         */
        composable(HomeRoutes.PRODUCTS) {
            OrderGate(
                isGuest = isGuest,
                onReady = { PlaceholderScreen("Productos") }, // ← sustituye por tu ProductsScreen real
                onAddAddress = { navController.navigate(HomeRoutes.AddressEdit.routeFor()) },
                onManageAddresses = { navController.navigate(HomeRoutes.ADDRESSES) },
                onRequestLogin = onRequestLogin,
                onRequestRegister = onRequestRegister
            )
        }

        /* ───────── Cuenta ───────── */
        composable(HomeRoutes.PROFILE)  { ProfileScreen() }
        composable(HomeRoutes.SETTINGS) { SettingsScreen() }
        composable(HomeRoutes.ORDERS)   { OrdersScreen() }

        /* ───────── Direcciones: listado ───────── */
        composable(HomeRoutes.ADDRESSES) {
            AddressListScreen(
                onBack = { navController.popBackStack() },
                onAddNew = { navController.navigate(HomeRoutes.AddressEdit.routeFor()) },
                onEdit = { aid -> navController.navigate(HomeRoutes.AddressEdit.routeFor(aid)) }
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
                onClose = { navController.popBackStack() }
            )
        }
    }
}

/* ═══════════ Placeholder mínimo ═══════════ */
@Composable
private fun PlaceholderScreen(title: String) {
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
