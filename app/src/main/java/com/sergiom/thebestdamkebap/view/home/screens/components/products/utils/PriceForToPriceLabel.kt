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

internal fun Long.toPriceLabel(
    locale: Locale = Locale.forLanguageTag("es-ES")
): String {
    val nf = NumberFormat.getCurrencyInstance(locale)
    return nf.format(this / 100.0) // céntimos → euros
}

