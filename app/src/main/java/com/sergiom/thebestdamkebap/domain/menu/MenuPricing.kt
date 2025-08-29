package com.sergiom.thebestdamkebap.domain.menu

import com.sergiom.thebestdamkebap.domain.catalog.Prices
import com.sergiom.thebestdamkebap.domain.order.OrderMode

/** Selección de un producto dentro de un grupo (con ingredientes quitados). */
data class SelectedProduct(
    val productId: String,
    val removedIngredients: Set<String> = emptySet()
)

/** Selecciones del usuario por grupo (acepta varios si max > 1). */
data class MenuSelections(
    val byGroup: Map<String /* groupId */, List<SelectedProduct>>
)

/** Desglose de precio del menú. */
data class PriceBreakdown(
    val base: Long,     // precio base del menú según modo
    val deltas: Long,   // suma de suplementos por las opciones elegidas
    val extras: Long = 0L // (si más adelante añades addons con precio)
) {
    @Suppress("unused")
    val total: Long get() = base + deltas + extras
}

/** Precio seguro (0 si falta). */
fun priceFor(prices: Prices?, mode: OrderMode): Long =
    when (mode) {
        OrderMode.PICKUP   -> prices?.pickup ?: 0L
        OrderMode.DELIVERY -> prices?.delivery ?: 0L
    }

/** Calcula el total del menú a partir de las selecciones. */
@Suppress("unused")
fun computeMenuTotal(menu: Menu, selections: MenuSelections, mode: OrderMode): PriceBreakdown {
    val base = priceFor(menu.prices, mode)
    var deltas = 0L

    menu.groups.forEach { group ->
        val chosen = selections.byGroup[group.id].orEmpty()
        chosen.forEach { sel ->
            val allowed = group.allowed.firstOrNull { it.productId == sel.productId }
            deltas += priceFor(allowed?.delta, mode)
        }
    }
    return PriceBreakdown(base = base, deltas = deltas)
}

sealed class MenuSelectionError {
    data class CountOutOfRange(
        val groupName: String,
        val min: Int,
        val max: Int,
        val actual: Int
    ) : MenuSelectionError()

    data class OptionNotAllowed(
        val groupName: String,
        val productId: String
    ) : MenuSelectionError()
}

/** Validación básica de selecciones (min/max y opciones permitidas). */
fun validateMenuSelections(menu: Menu, selections: MenuSelections): List<MenuSelectionError> {
    val errors = mutableListOf<MenuSelectionError>()
    menu.groups.forEach { g ->
        val chosen = selections.byGroup[g.id].orEmpty()

        if (chosen.size < g.min || chosen.size > g.max) {
            errors += MenuSelectionError.CountOutOfRange(
                groupName = g.name,
                min = g.min,
                max = g.max,
                actual = chosen.size
            )
        }

        val allowedIds = g.allowed.map { it.productId }.toSet()
        chosen.forEach { sel ->
            if (sel.productId !in allowedIds) {
                errors += MenuSelectionError.OptionNotAllowed(
                    groupName = g.name,
                    productId = sel.productId
                )
            }
        }
    }
    return errors
}
