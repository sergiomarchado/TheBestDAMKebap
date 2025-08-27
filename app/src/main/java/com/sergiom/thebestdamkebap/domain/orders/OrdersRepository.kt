package com.sergiom.thebestdamkebap.domain.orders

import com.sergiom.thebestdamkebap.domain.cart.CartState
import com.sergiom.thebestdamkebap.domain.order.OrderMode
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio de pedidos. Crea un pedido a partir del carrito.
 * Devuelve el ID del documento creado en Firestore.
 */
interface OrdersRepository {
    suspend fun submit(cart: CartState, mode: OrderMode, addressId: String?): String

    /** Observa los últimos pedidos de un usuario (ordenados por fecha desc). */
    fun observeMyOrders(uid: String, limit: Int = 20): Flow<List<OrderSummary>>
}
