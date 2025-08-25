package com.sergiom.thebestdamkebap.domain.catalog

data class Product(
    val id: String,
    val name: String,
    val description: String?,
    val imagePath: String?,
    val categoryId: String,
    val active: Boolean,
    val order: Long,
    val ingredients: List<String> = emptyList(),
    val prices: Prices
)
