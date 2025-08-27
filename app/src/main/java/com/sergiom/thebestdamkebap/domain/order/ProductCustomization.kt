package com.sergiom.thebestdamkebap.domain.order

/**
 * Personalización aplicada a un producto concreto.
 * Por ahora solo soportamos "quitar ingredientes".
 * Si algún día añades extras/salsas, puedes extender este data class.
 */
data class ProductCustomization(
    val removedIngredients: Set<String> = emptySet()
) {
    val isEmpty: Boolean get() = removedIngredients.isEmpty()

    @Suppress("unused")
    companion object {
        val None = ProductCustomization()
    }
}

