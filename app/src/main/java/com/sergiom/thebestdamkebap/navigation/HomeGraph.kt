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
        // HOME (HomeShell + nav interno)
        composable(HomeDestinations.HOME_MAIN) {
            HomeScreen(
                logoRes = R.drawable.ic_logo_home,
                onSignedOut = {
                    navController.navigate(Graph.AUTH) {
                        popUpTo(Graph.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onOpenCart = { /* navController.navigate("home/cart") */ },
                onOpenLogin = {
                    navController.navigate(Graph.AUTH) {
                        popUpTo(Graph.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onOpenRegister = {
                    navController.navigate("auth/entry?start=register") {
                        popUpTo(Graph.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
