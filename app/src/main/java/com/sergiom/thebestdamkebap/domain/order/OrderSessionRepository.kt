package com.sergiom.thebestdamkebap.domain.order

import kotlinx.coroutines.flow.StateFlow

interface OrderSessionRepository {
    /** Estado observable de la sesión del pedido. Siempre emite el último valor. */
    val context: StateFlow<OrderContext>

    /** Arranca/actualiza el pedido con el modo y (opcional) dirección. */
    suspend fun startOrder(mode: OrderMode, addressId: String?)

    /** Marca modo “solo estoy mirando” (no obliga a elegir nada). */
    suspend fun setBrowsingOnly()

    /** Limpia todo (logout, cancelar, etc.). */
    suspend fun clear()
}
