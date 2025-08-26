package com.sergiom.thebestdamkebap.domain.cart

import com.sergiom.thebestdamkebap.domain.order.OrderMode
import com.sergiom.thebestdamkebap.domain.order.ProductCustomization

/* Selecciones de un menú (reutilizada por diálogo y carrito) */
data class MenuSelection(
    val menuId: String,
    val selections: Map<String, List<Selection>> // groupId -> items elegidos
) {
    data class Selection(
        val productId: String,
        val customization: ProductCustomization? = null
    )
}

/* Ítems del carrito */
sealed interface CartItem {
    val lineId: String      // id único de línea
    val name: String        // snapshot del nombre
    val imagePath: String?  // snapshot imagen
    val unitPriceCents: Long
    val qty: Int
    val subtotalCents: Long get() = unitPriceCents * qty
}

data class ProductLine(
    override val lineId: String,
    val productId: String,
    override val name: String,
    override val imagePath: String?,
    override val unitPriceCents: Long,
    override val qty: Int,
    val customization: ProductCustomization? = null
) : CartItem

data class MenuLine(
    override val lineId: String,
    val menuId: String,
    override val name: String,
    override val imagePath: String?,
    override val unitPriceCents: Long,
    override val qty: Int,
    val selections: Map<String, List<MenuSelection.Selection>>
) : CartItem

data class CartState(
    val mode: OrderMode,
    val items: List<CartItem> = emptyList()
) {
    val totalItems: Int get() = items.sumOf { it.qty }
    val totalCents: Long get() = items.sumOf { it.subtotalCents }
}
