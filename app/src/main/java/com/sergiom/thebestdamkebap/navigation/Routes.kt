package com.sergiom.thebestdamkebap.navigation

/**
 * Conjunto de **rutas de navegación** centralizadas.
 *
 * Convenciones:
 * - `Graph.*` → rutas de **subgrafos** (nodos raíz de cada flujo).
 * - `Standalone.*` → pantallas **sueltas** fuera de un subgrafo (p. ej., Splash).
 * - `*Destinations.*` → rutas de **destinos hoja** dentro de cada grafo.
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
    // Más rutas de Home en el futuro: const val CART = "home/cart", etc.
}

