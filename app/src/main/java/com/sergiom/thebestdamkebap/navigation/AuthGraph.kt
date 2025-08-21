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
import androidx.compose.runtime.LaunchedEffect // ⬅️ nuevo import

/**
 * authGraph
 *
 * Subgrafo de navegación para el flujo de autenticación.
 *
 * Estructura:
 * - `Graph.AUTH` como ruta del subgrafo.
 * - Destino inicial: [AuthDestinations.LOGIN].
 * - Rutas incluidas: ENTRY (opcional), LOGIN y REGISTER.
 *
 * Novedad:
 * - `ENTRY` permite entrar al grafo indicando un parámetro `start=login|register`.
 *   Útil para abrir Auth directamente en Register **sin** saltarte el grafo.
 *
 * Comportamiento clave:
 * - Al autenticarse, se navega a `Graph.HOME` haciendo `popUpTo(Standalone.SPLASH)` con
 *   `inclusive = true` para **vaciar** el back stack hasta Splash (incluyéndolo), evitando
 *   que el usuario pueda volver a Splash/Login con el botón atrás.
 * - `launchSingleTop = true` evita instancias duplicadas del destino raíz si ya estuviera al tope.
 *
 * Colaboradores:
 * - [LoginScreen] y [RegisterScreen] que exponen callbacks de navegación.
 *
 * Requisitos:
 * - Las constantes de rutas (`Graph`, `AuthDestinations`, `Standalone`) deben ser únicas y estables.
 * - `Graph.HOME` debe existir y representar el grafo/pantalla raíz post-login.
 *
 * Uso recomendado:
 * - Entrar a Auth “normal”: `navController.navigate(Graph.AUTH)`.
 * - Entrar a Auth empezando en Register: `navController.navigate(AuthEntry.entryFor("register"))`.
 *
 * @param navController Controlador de navegación compartido a nivel de app.
 */
fun NavGraphBuilder.authGraph(
    navController: NavHostController
) {
    navigation(
        startDestination = AuthDestinations.LOGIN,
        route = Graph.AUTH
    ) {
        // --- ENTRY opcional: redirige a LOGIN/REGISTER según query param `start` ---
        composable(
            route = AuthEntry.ROUTE, // "auth/entry?start={start}"
            arguments = listOf(
                navArgument(AuthEntry.ARG_START) {
                    type = NavType.StringType
                    defaultValue = AuthEntry.START_LOGIN // "login"
                }
            )
        ) { backStackEntry ->
            // Leemos el parámetro `start` y decidimos el destino objetivo.
            val start = backStackEntry.arguments?.getString(AuthEntry.ARG_START)
                ?: AuthEntry.START_LOGIN
            val target = when (start) {
                AuthEntry.START_REGISTER -> AuthDestinations.REGISTER
                else -> AuthDestinations.LOGIN
            }
            // ✅ Navegación como side-effect para evitar ejecutar navigate() durante composición.
            LaunchedEffect(target) {
                navController.navigate(target) {
                    popUpTo(AuthEntry.ROUTE) { inclusive = true } // elimina ENTRY del back stack
                    launchSingleTop = true
                }
            }
        }

        composable(AuthDestinations.LOGIN) {
            LoginScreen(
                // Recurso estático para la cabecera del login.
                logoRes = R.drawable.ic_logo,
                // Usuario autenticado correctamente → navegar a HOME.
                // Se limpia el back stack hasta SPLASH (inclusive) para no volver a Auth.
                onAuthenticated = {
                    navController.navigate(Graph.HOME) {
                        popUpTo(Standalone.SPLASH) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onGoToRegister = {
                    // Ir al formulario de registro dentro del mismo subgrafo.
                    navController.navigate(AuthDestinations.REGISTER)
                }
            )
        }

        composable(AuthDestinations.REGISTER) {
            RegisterScreen(
                logoRes = R.drawable.ic_logo,
                // ✅ Volver a LOGIN de forma robusta (funciona vengas de donde vengas).
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

/**
 * Constantes/utilidades internas para la entrada parametrizable al grafo de Auth.
 *
 * Rutas:
 * - [ROUTE] → "auth/entry?start={start}" (query param opcional con default "login")
 *
 * Helpers:
 * - [entryFor] construye la ruta concreta para navegar: "auth/entry?start=register"
 */
private object AuthEntry {
    const val ARG_START = "start"
    const val START_LOGIN = "login"
    const val START_REGISTER = "register"

    // Ruta patrón registrada en el NavGraph (con argumento opcional).
    const val ROUTE = "auth/entry?start={$ARG_START}"

    // Builder para la ruta concreta con query param.
    fun entryFor(start: String): String = "auth/entry?start=$start"
}
