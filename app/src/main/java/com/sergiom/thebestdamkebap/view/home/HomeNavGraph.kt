package com.sergiom.thebestdamkebap.view.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.sergiom.thebestdamkebap.view.home.screens.AddressEditScreen
import com.sergiom.thebestdamkebap.view.home.screens.AddressListScreen
import com.sergiom.thebestdamkebap.view.home.screens.OrdersScreen
import com.sergiom.thebestdamkebap.view.home.screens.ProfileScreen
import com.sergiom.thebestdamkebap.view.home.screens.SettingsScreen

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

        // Direcciones (lista)
        composable(HomeRoutes.ADDRESSES) {
            AddressListScreen(
                onBack = { navController.popBackStack() },
                onAddNew = { navController.navigate(HomeRoutes.AddressEdit.routeFor()) },
                onEdit = { aid -> navController.navigate(HomeRoutes.AddressEdit.routeFor(aid)) }
            )
        }

        // Direcciones (editar / nueva)
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
            AddressEditScreen(aid = aid, onClose = { navController.popBackStack() })
        }
    }
}

/** Rutas internas de Home (una sola fuente de verdad). */
object HomeRoutes {
    const val HOME = "home"
    const val OFFERS = "offers"
    const val PRODUCTS = "products"

    // Cuenta
    const val PROFILE = "account/profile"
    const val SETTINGS = "account/settings"
    const val ORDERS = "account/orders"

    // Direcciones (integradas en el nav interno)
    const val ADDRESSES = "account/addresses"
    const val ADDRESS_EDIT = "account/addresses/edit?aid={aid}"
    object AddressEdit {
        const val ARG_AID = "aid"
        fun routeFor(aid: String? = null): String =
            if (aid.isNullOrBlank()) "account/addresses/edit"
            else "account/addresses/edit?aid=$aid"
    }
}
