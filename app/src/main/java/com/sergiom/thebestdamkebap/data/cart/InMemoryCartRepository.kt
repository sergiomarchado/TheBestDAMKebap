package com.sergiom.thebestdamkebap.data.cart

import com.sergiom.thebestdamkebap.domain.cart.*
import com.sergiom.thebestdamkebap.domain.catalog.Product
import com.sergiom.thebestdamkebap.domain.menu.Menu
import com.sergiom.thebestdamkebap.domain.order.OrderMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryCartRepository @Inject constructor() : CartRepository {

    private val _state = MutableStateFlow(CartState(mode = OrderMode.PICKUP))
    override val state: StateFlow<CartState> = _state

    override suspend fun setMode(mode: OrderMode) {
        // Opción 1: permitir cambiar modo y recalcular precios → lo simple: vaciar
        // Aquí mantenemos ítems y NO recalculamos (decisión UX pendiente). Si prefieres limpiar:
        // _state.value = CartState(mode = mode)
        _state.update { it.copy(mode = mode) }
    }

    override suspend fun addProduct(
        product: Product,
        customization: com.sergiom.thebestdamkebap.domain.order.ProductCustomization?,
        qty: Int
    ) {
        val mode = _state.value.mode
        val unit = priceFor(product, mode) ?: 0L
        val keyMatcher: (CartItem) -> Boolean = { item ->
            (item as? ProductLine)?.let {
                it.productId == product.id && it.customization == customization
            } ?: false
        }
        _state.update { curr ->
            val items = curr.items.toMutableList()
            val existing = items.indexOfFirst(keyMatcher)
            if (existing >= 0) {
                val line = items[existing] as ProductLine
                items[existing] = line.copy(qty = line.qty + qty)
            } else {
                items += ProductLine(
                    lineId = UUID.randomUUID().toString(),
                    productId = product.id,
                    name = product.name,
                    imagePath = product.imagePath,
                    unitPriceCents = unit,
                    qty = qty,
                    customization = customization
                )
            }
            curr.copy(items = items)
        }
    }

    override suspend fun addMenu(
        menu: Menu,
        selections: Map<String, List<MenuSelection.Selection>>,
        qty: Int
    ) {
        val mode = _state.value.mode
        val unit = menuUnitPrice(menu, selections, mode)

        val normalized = normalizeSelections(selections)
        val keyMatcher: (CartItem) -> Boolean = { item ->
            (item as? MenuLine)?.let {
                it.menuId == menu.id && normalizeSelections(it.selections) == normalized
            } ?: false
        }

        _state.update { curr ->
            val items = curr.items.toMutableList()
            val existing = items.indexOfFirst(keyMatcher)
            if (existing >= 0) {
                val line = items[existing] as MenuLine
                items[existing] = line.copy(qty = line.qty + qty)
            } else {
                items += MenuLine(
                    lineId = UUID.randomUUID().toString(),
                    menuId = menu.id,
                    name = menu.name,
                    imagePath = menu.imagePath,
                    unitPriceCents = unit,
                    qty = qty,
                    selections = selections
                )
            }
            curr.copy(items = items)
        }
    }

    override suspend fun updateQuantity(lineId: String, qty: Int) {
        if (qty <= 0) return remove(lineId)
        _state.update { curr ->
            curr.copy(items = curr.items.map {
                if (it.lineId == lineId) when (it) {
                    is ProductLine -> it.copy(qty = qty)
                    is MenuLine    -> it.copy(qty = qty)
                } else it
            })
        }
    }

    override suspend fun remove(lineId: String) {
        _state.update { curr -> curr.copy(items = curr.items.filterNot { it.lineId == lineId }) }
    }

    override suspend fun clear() { _state.update { it.copy(items = emptyList()) } }

    /* ---------- helpers de precio ---------- */

    private fun priceFor(p: Product, mode: OrderMode): Long? = when (mode) {
        OrderMode.DELIVERY -> p.prices.delivery ?: p.prices.pickup
        OrderMode.PICKUP   -> p.prices.pickup ?: p.prices.delivery
    }

    private fun menuUnitPrice(
        menu: Menu,
        selections: Map<String, List<MenuSelection.Selection>>,
        mode: OrderMode
    ): Long {
        val base = when (mode) {
            OrderMode.DELIVERY -> menu.prices.delivery ?: menu.prices.pickup
            OrderMode.PICKUP   -> menu.prices.pickup ?: menu.prices.delivery
        } ?: 0L

        val deltas = menu.groups.sumOf { group ->
            val chosen = selections[group.id].orEmpty()
            chosen.sumOf { sel ->
                val allowed = group.allowed.firstOrNull { it.productId == sel.productId }
                when (mode) {
                    OrderMode.DELIVERY -> allowed?.delta?.delivery ?: 0L
                    OrderMode.PICKUP   -> allowed?.delta?.pickup   ?: 0L
                }
            }
        }
        return base + deltas
    }

    // normaliza para comparar menús equivalentes (independiente del orden)
    private fun normalizeSelections(
        map: Map<String, List<MenuSelection.Selection>>
    ): Map<String, List<Pair<String, Set<String>>>> =
        map.toSortedMap().mapValues { (_, list) ->
            list.map { s ->
                s.productId to (s.customization?.removedIngredients ?: emptySet())
            }.sortedBy { it.first }
        }
}
