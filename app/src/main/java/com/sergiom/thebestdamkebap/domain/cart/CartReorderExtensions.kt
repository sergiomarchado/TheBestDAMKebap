// domain/cart/CartReorderExtensions.kt
package com.sergiom.thebestdamkebap.domain.cart

import com.sergiom.thebestdamkebap.domain.catalog.Product
import com.sergiom.thebestdamkebap.domain.menu.Menu
import com.sergiom.thebestdamkebap.domain.order.ProductCustomization
import com.sergiom.thebestdamkebap.domain.orders.ReorderLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Inserta en el carrito las líneas de un pedido anterior.
 * Carga productos/menús por ID y llama a tus propios addProduct / addMenu.
 */
suspend fun CartRepository.addFromReorderLines(
    lines: List<ReorderLine>,
    loadProductById: suspend (String) -> Product?,
    loadMenuById: suspend (String) -> Menu?,
) = withContext(Dispatchers.Default) {
    for (line in lines) {
        when (line) {
            is ReorderLine.Product -> {
                val product = loadProductById(line.productId) ?: continue
                val customization =
                    if (line.removedIngredients.isEmpty()) null
                    else ProductCustomization(line.removedIngredients.toSet())
                addProduct(product, customization, qty = line.qty)
            }
            is ReorderLine.Menu -> {
                val menu = loadMenuById(line.menuId) ?: continue
                val selections: Map<String, List<MenuSelection.Selection>> =
                    line.selections.mapValues { (_, list) ->
                        list.map { sel ->
                            MenuSelection.Selection(
                                productId = sel.productId,
                                customization =
                                    if (sel.removedIngredients.isEmpty()) null
                                    else ProductCustomization(sel.removedIngredients.toSet())
                            )
                        }
                    }
                addMenu(menu, selections = selections, qty = line.qty)
            }
        }
    }
}
