package com.sergiom.thebestdamkebap.domain.catalog

@Suppress("unused")
enum class CategoryType {PRODUCTS, MENUS}

data class Category(
    val id: String,
    val name: String,
    val order: Long,
    val active: Boolean,
    val imagePath: String? = null,
    val type: CategoryType = CategoryType.PRODUCTS
)

