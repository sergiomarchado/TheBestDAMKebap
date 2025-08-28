package com.sergiom.thebestdamkebap.navigation

/**
 * Definición centralizada de rutas de navegación para la app.
 *
 * Convenciones usadas:
 * - [Graph]: agrupa subgrafos raíz (ej. `auth`, `home`), cada uno puede tener varias pantallas.
 * - [Standalone]: pantallas que no dependen de un grafo (ej. splash inicial).
 * - `*Destinations`: destinos concretos (pantallas hoja) dentro de un grafo.
 *
 * Ventajas:
 * - Se evita duplicar strings en varios sitios → menos errores por typos.
 * - Si cambia un nombre de ruta, solo se modifica aquí.
 * - Mejora la legibilidad al saber de un vistazo cómo está estructurada la navegación.
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
