package com.sergiom.thebestdamkebap.view.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.sergiom.thebestdamkebap.navigation.HomeRoutes   // ⬅️ única fuente de verdad de rutas
import com.sergiom.thebestdamkebap.view.home.screens.AddressEditScreen
import com.sergiom.thebestdamkebap.view.home.screens.AddressListScreen
import com.sergiom.thebestdamkebap.view.home.screens.OrdersScreen
import com.sergiom.thebestdamkebap.view.home.screens.ProfileScreen
import com.sergiom.thebestdamkebap.view.home.screens.SettingsScreen

/**
 * Grafo de navegación interno de **Home**.
 *
 * Propósito:
 * - Define todas las rutas y pantallas que forman parte del flujo de Home.
 * - Se integra dentro de [HomeShell], y organiza navegación entre tabs (Inicio, Ofertas,
 *   Productos) y secciones de cuenta (Perfil, Direcciones, Pedidos, Ajustes).
 *
 * Características:
 * - `NavHost` con `startDestination = HomeRoutes.HOME`.
 * - Cada ruta se declara **una sola vez** en `navigation/HomeRoutes.kt` para evitar duplicados.
 * - Soporta navegación con argumentos opcionales (`aid` en edición de direcciones).
 *
 * Extensible:
 * - Sustituir pantallas `PlaceholderScreen` por implementaciones reales.
 * - Añadir subgrafos (ej. checkout) si crece la complejidad.
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
        // --- Portada / Inicio ---
        composable(HomeRoutes.HOME) { HomeStartScreen() }

        // --- Exploración (Ofertas / Productos) ---
        composable(HomeRoutes.OFFERS)   { PlaceholderScreen("Ofertas") }
        composable(HomeRoutes.PRODUCTS) { PlaceholderScreen("Productos") }

        // --- Cuenta ---
        composable(HomeRoutes.PROFILE)  { ProfileScreen() }
        composable(HomeRoutes.SETTINGS) { SettingsScreen() }
        composable(HomeRoutes.ORDERS)   { OrdersScreen() }

        // --- Lista de direcciones ---
        composable(HomeRoutes.ADDRESSES) {
            AddressListScreen(
                onBack = { navController.popBackStack() }, // compatibilidad
                onAddNew = { navController.navigate(HomeRoutes.AddressEdit.routeFor()) },
                onEdit = { aid -> navController.navigate(HomeRoutes.AddressEdit.routeFor(aid)) }
            )
        }

        // --- Editar / crear dirección ---
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

/* ═══════════ Pantallas mínimas para no dejar tabs en blanco ═══════════ */

@Composable
private fun HomeStartScreen() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Bienvenido 👋", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Esta es la portada de Home. Aquí irán banners, categorías, etc.")
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
    }
}
