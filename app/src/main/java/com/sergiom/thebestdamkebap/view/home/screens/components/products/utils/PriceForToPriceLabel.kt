package com.sergiom.thebestdamkebap.view.home.screens.components.products.utils

import com.sergiom.thebestdamkebap.domain.catalog.Product
import com.sergiom.thebestdamkebap.domain.order.OrderMode
import java.text.NumberFormat
import java.util.Locale

/* Helpers */

internal fun Product.priceFor(mode: OrderMode?): Long? = when (mode) {
    OrderMode.DELIVERY -> prices.delivery ?: prices.pickup
    OrderMode.PICKUP, null -> prices.pickup ?: prices.delivery
}

internal fun Long?.toPriceLabel(
    locale: Locale = Locale.forLanguageTag("es-ES")
): String {
    return this?.let {
        val nf = NumberFormat.getCurrencyInstance(locale)
        nf.format(it / 100.0)
    } ?: "—"
}

