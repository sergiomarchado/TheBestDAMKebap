// domain/orders/OrderSummary.kt
package com.sergiom.thebestdamkebap.domain.orders

import com.sergiom.thebestdamkebap.domain.order.OrderMode

/** Línea ligera para previsualizar el pedido en listados. */
data class OrderLinePreview(
    val qty: Int,
    val text: String
)

/** Línea “reordenable” (lo que guardamos en Firestore). */
sealed interface ReorderLine {
    data class Product(
        val productId: String,
        val name: String?,
        val imagePath: String?,
        val unitPriceCents: Int,
        val qty: Int,
        val removedIngredients: List<String>
    ) : ReorderLine

    data class Menu(
        val menuId: String,
        val name: String?,
        val imagePath: String?,
        val unitPriceCents: Int,
        val qty: Int,
        val selections: Map<String, List<Selection>>
    ) : ReorderLine {
        data class Selection(
            val productId: String,
            val removedIngredients: List<String>
        )
    }
}

/** Resumen para la lista, con previews y las líneas para reordenar. */
data class OrderSummary(
    val id: String,
    val createdAtMillis: Long?,   // serverTimestamp → puede ser null al primer tick
    val status: String,
    val totalCents: Long,
    val mode: OrderMode,
    val addressId: String?,
    val itemsCount: Int,
    val previews: List<OrderLinePreview> = emptyList(),
    val reorderLines: List<ReorderLine> = emptyList()
)
