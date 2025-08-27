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
 * - Registra el grafo [Graph.HOME] y su destino inicial [HomeDestinations.HOME_MAIN].
 * - Monta la pantalla de Home y define callbacks de salida a Auth.
 */
fun NavGraphBuilder.homeGraph(
    navController: NavHostController
) {
    navigation(
        startDestination = HomeDestinations.HOME_MAIN,
        route = Graph.HOME
    ) {
        composable(HomeDestinations.HOME_MAIN) {

            // Helper local para navegar a Auth limpiando el subgrafo de Home.
            val navigateToAuth: (String) -> Unit = { target ->
                navController.navigate(target) {
                    popUpTo(Graph.HOME) {
                        inclusive = true
                        // saveState = true // habilitar si quieres restaurar Home más adelante
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }

            HomeScreen(
                logoRes = R.drawable.ic_logo_home,
                onSignedOut = { navigateToAuth(AuthRoutes.entryForLogin()) },
                onOpenCart = { /* navController.navigate(HomeRoutes.CART) */ },
                onOpenRegister = { navigateToAuth(AuthRoutes.entryForRegister()) }
            )
        }
    }
}
