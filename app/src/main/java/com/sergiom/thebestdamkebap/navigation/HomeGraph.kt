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
 * - Registra el grafo [Graph.HOME] con destino inicial [HomeDestinations.HOME_MAIN].
 * - Muestra la pantalla principal de Home ([HomeScreen]).
 * - Define callbacks para salir de Home y volver al flujo de Auth.
 */
fun NavGraphBuilder.homeGraph(
    navController: NavHostController
) {
    navigation(
        startDestination = HomeDestinations.HOME_MAIN,
        route = Graph.HOME
    ) {
        composable(HomeDestinations.HOME_MAIN) {

            // Función auxiliar: navega a Auth limpiando el subgrafo de Home.
            // Esto evita que, tras cerrar sesión, el usuario pueda volver atrás al Home.
            val navigateToAuth: (String) -> Unit = { target ->
                navController.navigate(target) {

                    // Eliminamos el grafo de Home del back stack.
                    popUpTo(Graph.HOME) {
                        inclusive = true
                    }
                    launchSingleTop = true  // evita duplicar destinos en el stack.
                    restoreState = true     // intenta restaurar estado guardado si existía.
                }
            }

            HomeScreen(
                logoRes = R.drawable.ic_logo_home,
                // Callback al cerrar sesión: vuelve a Auth/Login.
                onSignedOut = { navigateToAuth(AuthRoutes.entryForLogin()) },
                // Callback al abrir el carrito (pendiente de implementación).
                onOpenCart = { },
                // Callback para abrir Auth/Register directamente desde Home.
                onOpenRegister = { navigateToAuth(AuthRoutes.entryForRegister()) }
            )
        }
    }
}
