package com.sergiom.thebestdamkebap.domain.order

/**
 * Contexto actual del pedido:
 * - Si [browsingOnly] = true → el usuario está “solo mirando” (no bloqueamos precios).
 * - Si [mode] = DELIVERY → [addressId] puede ser null hasta que vaya a confirmar/añadir al carrito.
 */
data class OrderContext(
    val mode: OrderMode? = null,
    val addressId: String? = null,
    val browsingOnly: Boolean = false
) {
    /** ¿Hay un pedido “activo”? Útil si quieres bloquear pasos de checkout. */
    val isActive: Boolean =
        !browsingOnly && when (mode) {
            OrderMode.PICKUP   -> true
            OrderMode.DELIVERY -> addressId != null
            null               -> false
        }
}

