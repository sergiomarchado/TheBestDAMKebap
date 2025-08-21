package com.sergiom.thebestdamkebap.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.sergiom.thebestdamkebap.R
import com.sergiom.thebestdamkebap.view.auth.LoginScreen
import com.sergiom.thebestdamkebap.view.auth.RegisterScreen

fun NavGraphBuilder.authGraph(
    navController: NavHostController
) {
    navigation(
        startDestination = AuthDestinations.LOGIN,
        route = Graph.AUTH
    ) {
        composable(AuthDestinations.LOGIN) {
            LoginScreen(
                logoRes = R.drawable.ic_logo,
                onAuthenticated = {
                    navController.navigate(Graph.HOME) {
                        popUpTo(Standalone.SPLASH) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onGoToRegister = {
                    navController.navigate(AuthDestinations.REGISTER)
                }
            )
        }

        composable(AuthDestinations.REGISTER) {
            RegisterScreen(
                logoRes = R.drawable.ic_logo,
                onBackToLogin = { navController.popBackStack() }
            )
        }
    }
}
