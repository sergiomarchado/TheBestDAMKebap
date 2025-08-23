// navigation/HomeRoutes.kt
package com.sergiom.thebestdamkebap.navigation

/**
 * Rutas internas del flujo de **Home**.
 *
 * Objetivo:
 * - Centralizar nombres de rutas y construcción de rutas con parámetros.
 * - Evitar duplicar strings “mágicos” repartidos por la app.
 *
 * Uso típico:
 * - En el NavHost de Home: `composable(HomeRoutes.PRODUCTS) { ... }`
 * - Para editar/crear dirección:
 *   - Nueva: `navController.navigate(HomeRoutes.AddressEdit.routeFor())`
 *   - Editar: `navController.navigate(HomeRoutes.AddressEdit.routeFor(aid))`
 */
object HomeRoutes {
    // Tabs principales
    const val HOME = "home"
    const val OFFERS = "offers"
    const val PRODUCTS = "products"

    // Secciones de cuenta
    const val PROFILE = "account/profile"
    const val SETTINGS = "account/settings"
    const val ORDERS = "account/orders"

    // Direcciones
    const val ADDRESSES = "account/addresses"
    const val ADDRESS_EDIT = "account/addresses/edit?aid={aid}"

    /**
     * Ayudas para construir la ruta concreta de edición/creación de direcciones.
     */
    object AddressEdit {
        const val ARG_AID = "aid"

        /**
         * @param aid ID de la dirección o `null`/vacío para crear una nueva.
         * @return Ruta navegable (sin placeholder), por ejemplo:
         *   - "account/addresses/edit" (nueva)
         *   - "account/addresses/edit?aid=abc123" (editar)
         */
        fun routeFor(aid: String? = null): String =
            if (aid.isNullOrBlank()) "account/addresses/edit"
            else "account/addresses/edit?aid=$aid"
    }
}
