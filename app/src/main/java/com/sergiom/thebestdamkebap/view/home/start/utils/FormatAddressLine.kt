package com.sergiom.thebestdamkebap.view.home.start.utils

internal fun formatAddressLine(street: String, number: String, city: String): String =
    listOfNotNull(street.ifBlank { null }, number.ifBlank { null }, city.ifBlank { null })
        .joinToString(", ")




