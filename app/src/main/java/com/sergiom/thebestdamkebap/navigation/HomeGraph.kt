// navigation/HomeGraph.kt
package com.sergiom.thebestdamkebap.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.sergiom.thebestdamkebap.R
import com.sergiom.thebestdamkebap.view.home.HomeScreen

fun NavGraphBuilder.homeGraph(
    navController: NavHostController
) {
    navigation(
        startDestination = HomeDestinations.HOME_MAIN,
        route = Graph.HOME
    ) {
        composable(HomeDestinations.HOME_MAIN) {
            HomeScreen(

                logoRes = R.drawable.ic_logo_home,

                // “Cerrar sesión” desde el top bar
                onSignedOut = {
                    navController.navigate(Graph.AUTH) {
                        popUpTo(Graph.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                },

                // Abrir carrito (si lo añades más adelante)
                onOpenCart = { /* navController.navigate("home/cart") */ },

                // Desde la píldora de usuario → Iniciar sesión
                onOpenLogin = {
                    navController.navigate(Graph.AUTH) {
                        popUpTo(Graph.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                },

                // Desde la píldora de usuario → Registrarse
                onOpenRegister = {
                    navController.navigate(AuthDestinations.REGISTER) {
                        popUpTo(Graph.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
