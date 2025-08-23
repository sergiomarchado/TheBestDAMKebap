package com.sergiom.thebestdamkebap.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.sergiom.thebestdamkebap.R
import com.sergiom.thebestdamkebap.view.auth.LoginScreen
import com.sergiom.thebestdamkebap.view.auth.RegisterScreen
import androidx.compose.runtime.LaunchedEffect

/**
 * Subgrafo de autenticación.
 *
 * Estructura:
 * - Ruta del grafo: [Graph.AUTH]
 * - Inicio: [AuthDestinations.LOGIN]
 * - Soporta entrada parametrizada vía [AuthRoutes] para abrir en login o register.
 *
 * Comportamiento:
 * - Al autenticarse se navega a [Graph.HOME] limpiando el back stack hasta [Standalone.SPLASH]
 *   (inclusive) para que no se pueda volver a Splash/Auth.
 * - La ruta `AuthRoutes.ENTRY` redirige (mediante `LaunchedEffect`) a LOGIN/REGISTER
 *   según el parámetro `start`.
 *
 * Requisitos:
 * - Rutas únicas y estables en [Graph], [Standalone], [AuthDestinations].
 * - [Graph.HOME] existe como grafo/pantalla raíz post-login.
 */
fun NavGraphBuilder.authGraph(
    navController: NavHostController
) {
    navigation(
        // Subgrafo con su ruta raíz y su destino inicial
        startDestination = AuthDestinations.LOGIN,
        route = Graph.AUTH
    ) {
        // --- ENTRY opcional: decide a dónde entrar (login/register) según query param `start` ---
        // Ruta patrón registrada: AuthRoutes.ENTRY = "auth/entry?start={start}"
        composable(
            route = AuthRoutes.ENTRY,
            arguments = listOf(
                navArgument(AuthRoutes.ARG_START) {
                    type = NavType.StringType
                    defaultValue = AuthRoutes.START_LOGIN // "login"
                }
            )
        ) { backStackEntry ->
            // Leemos el parámetro `start` y, a partir de él, elegimos destino dentro del subgrafo
            val start = backStackEntry.arguments?.getString(AuthRoutes.ARG_START)
                ?: AuthRoutes.START_LOGIN

            val target = when (start) {
                AuthRoutes.START_REGISTER -> AuthDestinations.REGISTER
                else -> AuthDestinations.LOGIN
            }

            // Navegación como efecto secundario (no durante la composición)
            LaunchedEffect(target) {
                navController.navigate(target) {
                    // Eliminamos ENTRY del back stack para que no se pueda volver a ella
                    popUpTo(AuthRoutes.ENTRY) { inclusive = true }
                    launchSingleTop = true // evita duplicados si ya estamos en `target`
                }
            }
        }

        // ========== LOGIN ==========
        composable(AuthDestinations.LOGIN) {
            LoginScreen(
                // Recurso de imagen (logo) para la cabecera
                logoRes = R.drawable.ic_logo,
                // Usuario autenticado → ir a HOME y limpiar Splash del historial
                onAuthenticated = {
                    navController.navigate(Graph.HOME) {
                        popUpTo(Standalone.SPLASH) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                // Ir a registro dentro del mismo subgrafo
                onGoToRegister = {
                    navController.navigate(AuthDestinations.REGISTER)
                }
            )
        }

        // ========== REGISTER ==========
        composable(AuthDestinations.REGISTER) {
            RegisterScreen(
                logoRes = R.drawable.ic_logo,
                // Volver a LOGIN: si existe en back stack, hacemos pop; si no, navegamos.
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
