// navigation/HomeGraph.kt
package com.sergiom.thebestdamkebap.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.sergiom.thebestdamkebap.R
import com.sergiom.thebestdamkebap.view.home.HomeScreen

/**
 * homeGraph
 *
 * Subgrafo raíz de la experiencia autenticada.
 *
 * Estructura:
 * - Ruta del grafo: [Graph.HOME].
 * - Destino inicial: [HomeDestinations.HOME_MAIN].
 *
 * Comportamiento clave:
 * - Exponen callbacks de navegación desde [HomeScreen] para cerrar sesión y redirigir
 *   a Auth, abrir login/registro y (futuro) abrir carrito.
 * - Al cerrar sesión o ir a pantallas de Auth, se limpia el back stack de Home con
 *   `popUpTo(Graph.HOME) { inclusive = true }` para **evitar volver** a Home con “atrás”.
 *
 * Requisitos/convenciones:
 * - Las rutas `Graph.HOME`, `Graph.AUTH` y `AuthDestinations.*` deben existir en el `NavHost`.
 * - `logoRes` es un recurso estático de branding (mantenido a nivel de UI).
 *
 * @param navController Controlador de navegación compartido.
 */
fun NavGraphBuilder.homeGraph(
    navController: NavHostController
) {
    navigation(
        startDestination = HomeDestinations.HOME_MAIN,
        route = Graph.HOME
    ) {
        composable(HomeDestinations.HOME_MAIN) {
            HomeScreen(

                // Logo de la pantalla principal.
                logoRes = R.drawable.ic_logo_home,

                // “Cerrar sesión” desde el top bar
                onSignedOut = {
                    // Al hacer sign-out limpiamos por completo el grafo HOME para impedir regresar con "atrás".
                    // Quedará Auth en la cima del back stack.
                    navController.navigate(Graph.AUTH) {
                        popUpTo(Graph.HOME) { inclusive = true }
                        // Evita duplicar Auth si ya estuviera en top.
                        launchSingleTop = true
                    }
                },

                // Abrir carrito (si lo añades más adelante)
                onOpenCart = { /* navController.navigate("home/cart") */ },
                // NOTE: Cuando implementes el carrito, define la ruta en este grafo (p. ej., HomeDestinations.CART)
                //       y añade su `composable`. Considera animaciones y comportamiento de back (desde detalle a cart).

                // Desde la píldora de usuario → Iniciar sesión
                onOpenLogin = {
                    // Caso: usuario no autenticado que quiere iniciar sesión desde Home.
                    // Limpia HOME y navega a AUTH para evitar que quede historial inconsistente.
                    navController.navigate(Graph.AUTH) {
                        popUpTo(Graph.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                },

                // Desde la píldora de usuario → Registrarse
                onOpenRegister = {
                    // Entramos en el grafo de Auth **arrancando en Register** a través de ENTRY.
                    // Usamos la ruta concreta "auth/entry?start=register" para no depender de un símbolo público.
                    navController.navigate("auth/entry?start=register") {
                        popUpTo(Graph.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}