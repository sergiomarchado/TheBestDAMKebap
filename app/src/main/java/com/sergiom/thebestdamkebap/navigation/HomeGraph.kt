// navigation/HomeGraph.kt
package com.sergiom.thebestdamkebap.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.sergiom.thebestdamkebap.R
import com.sergiom.thebestdamkebap.view.home.HomeScreen

/**
 * Subgrafo de la sección **Home**.
 *
 * Qué hace:
 * - Registra el grafo `Graph.HOME` y su destino inicial [HomeDestinations.HOME_MAIN].
 * - Monta la pantalla de Home (shell + nav interno) y define callbacks para salir a Auth.
 *
 * Back stack:
 * - Al salir de Home se usa `popUpTo(Graph.HOME){ inclusive = true }` para limpiarlo, evitando
 *   volver con el botón atrás tras un logout o al abrir el flujo de Auth.
 */
fun NavGraphBuilder.homeGraph(
    navController: NavHostController
) {
    navigation(
        startDestination = HomeDestinations.HOME_MAIN,
        route = Graph.HOME
    ) {
        composable(HomeDestinations.HOME_MAIN) {

            // Helper local para evitar duplicar el mismo patrón de navegación a Auth.
            val navigateToAuth: (String) -> Unit = { target ->
                navController.navigate(target) {
                    popUpTo(Graph.HOME) { inclusive = true } // limpia tod Home del stack
                    launchSingleTop = true                    // evita duplicados
                    restoreState = true
                }
            }

            HomeScreen(
                logoRes = R.drawable.ic_logo_home,

                // Cerrar sesión desde Home → abrir Auth empezando en Login y limpiar Home del historial.
                onSignedOut = {
                    navigateToAuth(AuthRoutes.entryForLogin())
                },

                // (Placeholder) Carrito: cuando lo tengas, mantenlo en el grafo interno de Home.
                onOpenCart = { /* navController.navigate("home/cart") */ },

                // Invitado pulsa “Iniciar sesión” → abrir Auth en Login (sin dejar Home en el stack).
                onOpenLogin = {
                    navigateToAuth(AuthRoutes.entryForLogin())
                },

                // Invitado pulsa “Registrarse” → abrir Auth directamente en Register.
                onOpenRegister = {
                    navigateToAuth(AuthRoutes.entryForRegister())
                }
            )
        }
    }
}
