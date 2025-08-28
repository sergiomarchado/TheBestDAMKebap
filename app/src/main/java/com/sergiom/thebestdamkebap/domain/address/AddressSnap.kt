// domain/address/AddressSnap.kt
package com.sergiom.thebestdamkebap.domain.address

/**
 * Snapshot inmutable de la dirección en el momento de crear el pedido.
 * Lo guardamos dentro del documento de /orders para auditoría/entrega.
 */
data class AddressSnap(
    val label: String? = null,
    val recipientName: String? = null,
    val phone: String,              // obligatorio
    val street: String,             // obligatorio
    val number: String,             // obligatorio
    val floorDoor: String? = null,
    val city: String,               // obligatorio
    val province: String? = null,
    val postalCode: String,         // obligatorio
    val notes: String? = null,
    val lat: Double? = null,
    val lng: Double? = null
)
