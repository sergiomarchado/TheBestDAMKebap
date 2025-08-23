package com.sergiom.thebestdamkebap.navigation

/**
 * Conjunto centralizado de rutas de navegación.
 *
 * Convenciones:
 * - [Graph]: subgrafos raíz (agrupan pantallas relacionadas).
 * - [Standalone]: pantallas únicas fuera de subgrafos (ej. Splash).
 * - `*Destinations`: destinos hoja dentro de cada grafo.
 *
 * Ejemplos de uso:
 * ```
 * navController.navigate(Graph.AUTH)
 * navController.navigate(AuthDestinations.LOGIN)
 * ```
 *
 */
object Graph {
    const val AUTH = "auth"
    const val HOME = "home"
}

object Standalone {
    const val SPLASH = "splash"
}

object AuthDestinations {
    const val LOGIN = "login"
    const val REGISTER = "register"
}

object HomeDestinations {
    const val HOME_MAIN = "home/main"

}

