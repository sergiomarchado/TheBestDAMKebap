// data/address/Address.kt
package com.sergiom.thebestdamkebap.data.address

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/** Dirección almacenada en `/users/{uid}/addresses/{id}`. */
data class Address(
    @get:Exclude val id: String = "",
    val label: String? = null,        // "Casa", "Trabajo"…
    val recipientName: String? = null,
    val phone: String? = null,        // móvil de contacto
    val street: String = "",          // calle/avenida
    val number: String = "",          // número / km / s/n
    val floorDoor: String? = null,    // piso, puerta
    val city: String = "",
    val province: String? = null,     // opcional
    val postalCode: String = "",      // 5 dígitos (ES)
    val notes: String? = null,        // indicaciones al repartidor
    val lat: Double? = null,          // opcional para geocoding futuro
    val lng: Double? = null,
    @ServerTimestamp var createdAt: Date? = null,
    @ServerTimestamp var updatedAt: Date? = null
)

