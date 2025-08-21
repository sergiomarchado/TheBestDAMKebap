// navigation/AppNavHost.kt
package com.sergiom.thebestdamkebap.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
/**
 * AppNavHost
 *
 * Punto único de composición para la navegación basada en Navigation-Compose.
 * Orquesta el grafo raíz y delega en subgrafos especializados (splash, auth, home).
 *
 * Responsabilidades:
 * - Crear (o recibir) un [NavHostController] que gestione el back stack de la app.
 * - Definir la ruta de inicio ([Standalone.SPLASH]) y registrar los subgrafos.
 * - Mantener un **único** `NavHost` en la raíz para evitar back stacks duplicados.
 *
 * Colaboradores:
 * - `splashRoute(...)`: destino/flow inicial que decide a dónde ir (auth/home).
 * - `authGraph(...)`: flujo de autenticación (login, registro, recuperación…).
 * - `homeGraph(...)`: grafo principal tras autenticación.
 *
 * Notas de uso:
 * - El `navController` es **inyectable** para tests/previews al ser parámetro opcional.
 * - `rememberNavController()` garantiza estabilidad del controlador entre recomposiciones.
 * - Si tras login quieres limpiar historial (evitar volver a Splash/Auth), usa `popUpTo`
 *   con `inclusive = true` al navegar hacia Home (en el lugar correspondiente del grafo).
 *
 * @param navController Controlador de navegación. Si no se pasa, se crea uno recordado.
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
