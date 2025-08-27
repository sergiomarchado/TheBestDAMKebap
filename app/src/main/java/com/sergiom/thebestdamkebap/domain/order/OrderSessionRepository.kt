package com.sergiom.thebestdamkebap.domain.order

import kotlinx.coroutines.flow.StateFlow

/**
 * Almacena y expone el contexto de la **sesión de pedido** (modo, dirección, browsing).
 *
 * Debe ser **única fuente de verdad** y sobrevivir al proceso (DataStore/DB).
 */
interface OrderSessionRepository {
    /** Estado observable y caliente (último valor siempre disponible). */
    val context: StateFlow<OrderContext>

    /** Arranca/actualiza el pedido con el [mode] y (opcional) [addressId]. */
    suspend fun startOrder(mode: OrderMode, addressId: String?)

    /** Marca “solo estoy mirando” y limpia modo/dirección. */
    suspend fun setBrowsingOnly()

    /** Limpia toda la sesión (logout/cancelar). */
    suspend fun clear()
}
