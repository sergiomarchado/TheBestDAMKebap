// navigation/HomeRoutes.kt
package com.sergiom.thebestdamkebap.navigation

/**
 * Rutas internas del flujo de **Home**.
 *
 * Objetivo:
 * - Centralizar nombres de rutas y construcción de rutas con parámetros.
 * - Evitar duplicar strings “mágicos” repartidos por la app.
 *
 * Notas:
 * - `PRODUCTS_GATE` es una ruta intermedia que muestra un “gate” (selección
 *   de modo/dirección o “solo estoy mirando”) antes de entrar en `PRODUCTS`.
 */
object HomeRoutes {
    // Tabs principales
    const val HOME = "home"
    const val OFFERS = "offers"

    /** Ruta intermedia: pantalla/hoja que pide modo/dirección antes de Productos. */
    const val PRODUCTS_GATE = "products_gate"

    /** Pantalla real de productos. */
    const val PRODUCTS = "products"

    // Secciones de cuenta
    const val PROFILE = "account/profile"
    const val SETTINGS = "account/settings"
    const val ORDERS = "account/orders"
    const val CART = "cart"

    // Direcciones
    const val ADDRESSES = "account/addresses"

    /** Ruta con placeholder para argumento opcional `aid`. */
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
