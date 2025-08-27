package com.sergiom.thebestdamkebap.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.navArgument
import com.sergiom.thebestdamkebap.R
import com.sergiom.thebestdamkebap.view.auth.LoginScreen
import com.sergiom.thebestdamkebap.view.auth.RegisterScreen
import com.sergiom.thebestdamkebap.viewmodel.auth.AuthViewModel

/**
 * Subgrafo de autenticación.
 *
 * Estructura:
 * - Ruta: [Graph.AUTH]
 * - Inicio: [AuthDestinations.LOGIN]
 * - Entrada parametrizada: [AuthRoutes.ENTRY] con query opcional `start` (login/register).
 */
fun NavGraphBuilder.authGraph(
    navController: NavHostController
) {
    navigation(
        startDestination = AuthDestinations.LOGIN,
        route = Graph.AUTH
    ) {
        // ENTRY opcional: decide a dónde entrar (login/register) según query param `start`.
        composable(
            route = AuthRoutes.ENTRY,
            arguments = listOf(
                navArgument(AuthRoutes.ARG_START) {
                    type = NavType.StringType
                    defaultValue = AuthRoutes.START_LOGIN
                }
            )
        ) { backStackEntry ->
            val start = backStackEntry.arguments?.getString(AuthRoutes.ARG_START)
                ?: AuthRoutes.START_LOGIN

            val target = when (start) {
                AuthRoutes.START_REGISTER -> AuthDestinations.REGISTER
                else -> AuthDestinations.LOGIN
            }

            // Side-effect de navegación (evita navegar durante composición).
            LaunchedEffect(target) {
                navController.navigate(target) {
                    // Quita ENTRY del back stack.
                    popUpTo(AuthRoutes.ENTRY) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }

        // LOGIN
        composable(AuthDestinations.LOGIN) {
            val vm: AuthViewModel = hiltViewModel()
            LoginScreen(
                logoRes = R.drawable.ic_logo,
                onAuthenticated = {
                    navController.navigate(Graph.HOME) {
                        // Limpia todo el subgrafo de Auth.
                        popUpTo(Graph.AUTH) { inclusive = true }
                        launchSingleTop = true
                        // restoreState solo tiene efecto si previamente se guardó estado con saveState.
                        restoreState = true
                    }
                },
                onGoToRegister = { navController.navigate(AuthDestinations.REGISTER) },
                onContinueAsGuest = {
                    vm.continueAsGuest()
                    navController.navigate(Graph.HOME) {
                        popUpTo(Graph.AUTH) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // REGISTER
        composable(AuthDestinations.REGISTER) {
            RegisterScreen(
                logoRes = R.drawable.ic_logo,
                onBackToLogin = {
                    val popped = navController.popBackStack(
                        AuthDestinations.LOGIN,
                        /* inclusive = */ false
                    )
                    if (!popped) {
                        navController.navigate(AuthDestinations.LOGIN) { launchSingleTop = true }
                    }
                }
            )
        }
    }
}
