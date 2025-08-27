package com.sergiom.thebestdamkebap.domain.orders

import com.sergiom.thebestdamkebap.domain.cart.CartState
import com.sergiom.thebestdamkebap.domain.order.OrderMode

/**
 * Repositorio de pedidos. Crea un pedido a partir del carrito.
 * Devuelve el ID del documento creado en Firestore.
 */
interface OrdersRepository {
    suspend fun submit(cart: CartState, mode: OrderMode, addressId: String?): String
}
