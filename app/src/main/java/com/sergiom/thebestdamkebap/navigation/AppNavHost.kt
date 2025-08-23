// navigation/AppNavHost.kt
package com.sergiom.thebestdamkebap.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
/**
 * Punto único de composición para la navegación con Navigation-Compose.
 *
 * Responsabilidad:
 * - Crea (o recibe) el [NavHostController] y define el grafo raíz con `splash`, `auth` y `home`.
 * - Mantiene un único `NavHost` para evitar back stacks duplicados.
 *
 * Notas:
 * - En login/registro, al saltar a Home, usa `popUpTo(Standalone.SPLASH) { inclusive = true }`
 *   y considera `saveState/restoreState` para preservar estado al volver a tabs/listas.
 *
 * @param navController Controlador de navegación (inyectable para tests/previews).
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
