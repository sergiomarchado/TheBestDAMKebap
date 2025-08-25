package com.sergiom.thebestdamkebap.domain.catalog

data class Category(
    val id: String,
    val name: String,
    val order: Long,
    val active: Boolean,
    val imagePath: String? = null
)

