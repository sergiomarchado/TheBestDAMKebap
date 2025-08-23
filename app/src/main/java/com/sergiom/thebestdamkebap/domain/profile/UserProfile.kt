package com.sergiom.thebestdamkebap.domain.profile

/** Modelo de dominio (sin anotaciones Firebase). */
data class UserProfile(
    val uid: String,
    val email: String? = null,
    val givenName: String? = null,
    val familyName: String? = null,
    val phone: String? = null,
    /** Epoch millis (o null si no hay fecha). */
    val birthDateMillis: Long? = null,
    /** id de la dirección predeterminada (subcolección /addresses). */
    val defaultAddressId: String? = null,
    val createdAtMillis: Long? = null,
    val updatedAtMillis: Long? = null
)

/** Entrada de actualización parcial (merge). */
data class ProfileInput(
    val givenName: String? = null,
    val familyName: String? = null,
    val phone: String? = null,
    val birthDateMillis: Long? = null
)
