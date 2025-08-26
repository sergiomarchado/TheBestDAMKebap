package com.sergiom.thebestdamkebap.domain.cart

import com.sergiom.thebestdamkebap.domain.catalog.Product
import com.sergiom.thebestdamkebap.domain.menu.Menu
import com.sergiom.thebestdamkebap.domain.order.OrderMode
import com.sergiom.thebestdamkebap.domain.order.ProductCustomization
import kotlinx.coroutines.flow.StateFlow

interface CartRepository {
    val state: StateFlow<CartState>

    suspend fun setMode(mode: OrderMode)

    suspend fun addProduct(
        product: Product,
        customization: ProductCustomization? = null,
        qty: Int = 1
    )

    suspend fun addMenu(
        menu: Menu,
        selections: Map<String, List<MenuSelection.Selection>>,
        qty: Int = 1
    )

    suspend fun updateQuantity(lineId: String, qty: Int)
    suspend fun remove(lineId: String)
    suspend fun clear()
}
