package com.sergiom.thebestdamkebap.navigation

/**
 * Rutas internas del flujo de **Home**.
 *
 * Objetivo:
 * - Centralizar todos los nombres de rutas de la sección Home.
 * - Facilitar la construcción de rutas parametrizadas (con argumentos).
 *
 * Ventajas:
 * - Evita duplicar strings mágicos por toda la app.
 * - Si cambia una ruta, solo hay que modificarla aquí.
 * - Mejora la legibilidad del grafo de navegación.
 */
object HomeRoutes {
    // Tabs principales
    const val HOME = "home"
    const val OFFERS = "offers"

    // Gate → selecciona modo/dirección antes de Productos.
    @Suppress("unused")
    const val PRODUCTS_GATE = "products_gate"
    const val PRODUCTS = "products"

    // Cuenta
    const val PROFILE = "account/profile"
    const val SETTINGS = "account/settings"
    const val ORDERS = "account/orders"

    // Carrito
    const val CART = "cart"

    // Direcciones
    const val ADDRESSES = "account/addresses"

    /**
     * Ruta de edición de dirección.
     * Puede incluir un argumento opcional `aid` (address id) para editar
     * una dirección existente, o abrir vacía para crear una nueva.
     */
    const val ADDRESS_EDIT = "account/addresses/edit?aid={aid}"

    object AddressEdit {
        /** Nombre del argumento que representa el ID de la dirección. */
        const val ARG_AID = "aid"

        /**
         * Construye la ruta concreta para la pantalla de edición de dirección.
         *
         * @param aid id de dirección a editar; si es `null` o vacío,
         *            se devuelve la ruta para crear una nueva dirección.
         */
        fun routeFor(aid: String? = null): String =
            if (aid.isNullOrBlank()) "account/addresses/edit"
            else "account/addresses/edit?aid=$aid"
    }
}
