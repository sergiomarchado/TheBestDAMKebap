package com.sergiom.thebestdamkebap.navigation

/**
 * Rutas internas del flujo de **Home**.
 *
 * Centraliza nombres de rutas y construcción de rutas con parámetros.
 */
object HomeRoutes {
    // Tabs principales
    const val HOME = "home"
    const val OFFERS = "offers"

    // Gate → selecciona modo/dirección antes de Productos.
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
    const val ADDRESS_EDIT = "account/addresses/edit?aid={aid}"

    object AddressEdit {
        const val ARG_AID = "aid"
        fun routeFor(aid: String? = null): String =
            if (aid.isNullOrBlank()) "account/addresses/edit"
            else "account/addresses/edit?aid=$aid"
    }
}
