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
 * - Ruta del grafo: [Graph.AUTH]
 * - Pantalla inicial: [AuthDestinations.LOGIN]
 * - Entrada indirecta: [AuthRoutes.ENTRY] → permite abrir directamente LOGIN o REGISTER
 *   en función de un parámetro `start`.
 *
 * Contiene:
 * - [AuthDestinations.LOGIN] → pantalla de login.
 * - [AuthDestinations.REGISTER] → pantalla de registro.
 */
fun NavGraphBuilder.authGraph(
    navController: NavHostController
) {
    navigation(
        startDestination = AuthDestinations.LOGIN,
        route = Graph.AUTH
    ) {
        // ──────────────────────────────────────────────────────────────
        // ENTRY opcional: redirige a login o registro según query param.
        // ──────────────────────────────────────────────────────────────
        composable(
            route = AuthRoutes.ENTRY,
            arguments = listOf(
                navArgument(AuthRoutes.ARG_START) {
                    type = NavType.StringType
                    defaultValue = AuthRoutes.START_LOGIN
                }
            )
        ) { backStackEntry ->

            // Leemos el argumento `start` de la entrada de navegación
            val start = backStackEntry.arguments?.getString(AuthRoutes.ARG_START)
                ?: AuthRoutes.START_LOGIN

            // Determinamos el destino final según el valor de `start`.
            val target = when (start) {
                AuthRoutes.START_REGISTER -> AuthDestinations.REGISTER
                else -> AuthDestinations.LOGIN
            }

            // Navegamos con efecto lateral (no durante composición).
            LaunchedEffect(target) {
                navController.navigate(target) {
                    // Sacamos ENTRY del back stack: no volveremos a esta pseudo-ruta.
                    popUpTo(AuthRoutes.ENTRY) { inclusive = true }
                    launchSingleTop = true  // evita duplicados en el stack
                }
            }
        }

        // ──────────────────────────────────────────────────────────────
        // LOGIN
        // ──────────────────────────────────────────────────────────────
        composable(AuthDestinations.LOGIN) {
            val vm: AuthViewModel = hiltViewModel()
            LoginScreen(
                logoRes = R.drawable.ic_logo,

                // Al autenticar con éxito, vamos al grafo HOME y limpiamos AUTH del back stack.
                onAuthenticated = {
                    navController.navigate(Graph.HOME) {
                        // Limpia todo el subgrafo de Auth.
                        popUpTo(Graph.AUTH) { inclusive = true }
                        launchSingleTop = true
                        // restoreState permite volver a un estado previo si estaba guardado.
                        restoreState = true
                    }
                },

                // Enlace para ir a la pantalla de registro.
                onGoToRegister = { navController.navigate(AuthDestinations.REGISTER) },

                // Acceso como invitado → notifica al VM y navega a HOME.
                onContinueAsGuest = {
                    vm.continueAsGuest()
                    navController.navigate(Graph.HOME) {
                        popUpTo(Graph.AUTH) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // ──────────────────────────────────────────────────────────────
        // REGISTER
        // ──────────────────────────────────────────────────────────────
        composable(AuthDestinations.REGISTER) {
            RegisterScreen(
                logoRes = R.drawable.ic_logo,

                // Botón "volver al login": intentamos hacer popBackStack.
                // Si no está en el back stack (caso raro), navegamos de nuevo a LOGIN.
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
