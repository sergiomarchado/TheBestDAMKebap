package com.sergiom.thebestdamkebap.view.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.sergiom.thebestdamkebap.view.home.screens.OrdersScreen
import com.sergiom.thebestdamkebap.view.home.screens.ProfileScreen
import com.sergiom.thebestdamkebap.view.home.screens.SettingsScreen

/**
 * NavHost interno de Home. Incluye las secciones de exploración y las de cuenta.
 */
@Composable
fun HomeNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoutes.HOME,
        modifier = modifier
    ) {
        // Exploración
        composable(HomeRoutes.HOME)     { /* TODO: contenido real */ }
        composable(HomeRoutes.OFFERS)   { /* TODO: contenido real */ }
        composable(HomeRoutes.PRODUCTS) { /* TODO: contenido real */ }

        // Cuenta
        composable(HomeRoutes.PROFILE)  { ProfileScreen() }
        composable(HomeRoutes.SETTINGS) { SettingsScreen() }
        composable(HomeRoutes.ORDERS)   { OrdersScreen() }
        // Nota: "Direcciones" lo tratamos como sheet desde el drawer (sin ruta),
        // pero si en el futuro prefieres pantalla completa, añade:
        // composable(HomeRoutes.ADDRESSES) { AddressesScreen() }
    }
}

/** Rutas internas de Home (constantes centralizadas). */
object HomeRoutes {
    const val HOME = "home"
    const val OFFERS = "offers"
    const val PRODUCTS = "products"

    // Cuenta
    const val PROFILE = "account/profile"
    const val SETTINGS = "account/settings"
    const val ORDERS = "account/orders"
    // const val ADDRESSES = "account/addresses" // si lo llevas a pantalla completa en el futuro
}
