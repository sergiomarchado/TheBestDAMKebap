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
    // NUEVO: Direcciones
    const val ADDRESS_LIST = "home/addresses"
    const val ADDRESS_EDIT = "home/addresses/edit?aid={aid}"
    object AddressEdit {
        const val ARG_AID = "aid"
        fun routeFor(aid: String? = null): String =
            if (aid.isNullOrBlank()) "home/addresses/edit"
            else "home/addresses/edit?aid=$aid"
    }

}

