package com.sergiom.thebestdamkebap.domain.address

// Modelo de dominio (sin anotaciones de Firebase)
data class Address(
    val id: String = "",
    val label: String? = null,
    val recipientName: String? = null,
    val phone: String? = null,
    val street: String = "",
    val number: String = "",
    val floorDoor: String? = null,
    val city: String = "",
    val province: String? = null,
    val postalCode: String = "",
    val notes: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val createdAtMillis: Long? = null,
    val updatedAtMillis: Long? = null
)

// Entrada de alta/edición en dominio
data class AddressInput(
    val label: String? = null,
    val recipientName: String? = null,
    val phone: String? = null,
    val street: String? = null,
    val number: String? = null,
    val floorDoor: String? = null,
    val city: String? = null,
    val province: String? = null,
    val postalCode: String? = null,
    val notes: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
)
