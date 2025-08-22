package com.sergiom.thebestdamkebap.data.profile

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Perfil de usuario guardado en Firestore: `/users/{uid}`.
 *
 * Notas:
 * - `email` se denormaliza para facilitar listados desde Angular/Backoffice (no usamos Auth aquí).
 * - `birthDate` se almacena como `Date` (Firestore Timestamp). En UI lo formatearemos.
 * - `defaultAddressId` referencia (por id) la dirección predeterminada en `/users/{uid}/addresses`.
 * - `createdAt/updatedAt` se rellenan en servidor con @ServerTimestamp.
 */
data class UserProfile(
    val uid: String = "",
    val email: String? = null,
    val givenName: String? = null,
    val familyName: String? = null,
    val phone: String? = null,
    val birthDate: Date? = null,
    val defaultAddressId: String? = null,   // ← NUEVO: id de la dirección predeterminada
    @ServerTimestamp var createdAt: Date? = null,
    @ServerTimestamp var updatedAt: Date? = null
)

/**
 * Entrada de actualización parcial de perfil (merge).
 * Usa `null` para “borrar” un valor (p. ej., `phone = null`).
 */
data class ProfileInput(
    val givenName: String? = null,
    val familyName: String? = null,
    val phone: String? = null,
    /** Epoch millis para la fecha de nacimiento; null para borrar. */
    val birthDateMillis: Long? = null,
)
