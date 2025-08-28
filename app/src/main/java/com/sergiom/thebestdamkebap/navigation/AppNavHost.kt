package com.sergiom.thebestdamkebap.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController

/**
 * Punto único de composición para la navegación con Navigation-Compose.
 *
 * Responsabilidades:
 * - Crear (o reutilizar) el [NavHostController] que gestiona el back stack de pantallas.
 * - Declarar el grafo raíz con las rutas principales: `splash`, `auth` y `home`.
 * - Mantener un único `NavHost` en la jerarquía para evitar back stacks duplicados.
 *
 * Testing/Previews:
 * - [navController] es inyectable: permite usar uno controlado en tests/previews,
 *   y el por defecto (`rememberNavController()`) en ejecución normal.
 *
 * Notas:
 * - `startDestination` define la primera pantalla del grafo (aquí `SPLASH`).
 * - `splashRoute`, `authGraph` y `homeGraph` añaden destinos al grafo actual.
 *   Los dos últimos son subgrafos; cada uno puede tener su propio `startDestination`.
 */
@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Standalone.SPLASH
    ) {
        splashRoute(navController)
        authGraph(navController)
        homeGraph(navController)
    }
}
