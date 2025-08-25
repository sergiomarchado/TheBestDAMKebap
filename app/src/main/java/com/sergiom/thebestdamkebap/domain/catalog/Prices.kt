package com.sergiom.thebestdamkebap.domain.catalog

data class Prices(
    val pickup: Long?,   // céntimos (650 -> 6,50 €)
    val delivery: Long?  // céntimos
)
