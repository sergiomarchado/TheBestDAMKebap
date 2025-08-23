package com.sergiom.thebestdamkebap.domain.profile

/** Modelo de dominio (sin anotaciones Firebase). */
data class UserProfile(
    val uid: String,
    val email: String?,
    val givenName: String?,
    val familyName: String?,
    val phone: String?,
    /** Epoch millis (o null si no hay fecha). */
    val birthDateMillis: Long?,
    /** id de la dirección predeterminada (subcolección /addresses). */
    val defaultAddressId: String?,
    val createdAtMillis: Long?,
    val updatedAtMillis: Long?
)

/** Entrada de actualización parcial (merge). */
data class ProfileInput(
    val givenName: String? = null,
    val familyName: String? = null,
    val phone: String? = null,
    val birthDateMillis: Long? = null
)
