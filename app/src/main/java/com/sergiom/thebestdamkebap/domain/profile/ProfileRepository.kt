package com.sergiom.thebestdamkebap.domain.profile

import kotlinx.coroutines.flow.Flow


/**
 * Contrato de acceso al **perfil de usuario** (p. ej., en Firestore).
 *
 * Objetivo:
 * - Ofrecer a la app una forma simple de **observar** y **actualizar** el perfil sin
 *   preocuparse por detalles de la base de datos (timestamps, merges, etc.).
 *
 * Convenciones de campos (esperadas por la implementación):
 * - `uid`            → debe coincidir con el ID del documento (`/users/{uid}`).
 * - `email`          → email visible de la cuenta (si se conoce).
 * - `givenName`, `familyName`, `phone`, `birthDate`… → datos opcionales del perfil.
 * - `createdAt`      → solo se fija al crear; **no** se toca en updates.
 * - `updatedAt`      → se actualiza en cada escritura (serverTimestamp).
 *
 * Notas:
 * - Las escrituras deben usar `serverTimestamp()` para `createdAt/updatedAt`.
 * - En updates, **no** se debe modificar `createdAt`.
 * - Cuando un parámetro es `null` (p. ej. `email`), la implementación **no** debería
 *   sobreescribir el campo con `null`; simplemente lo ignora (deja el valor existente).
 */
interface ProfileRepository {

    /** Observa el perfil del usuario (o null si no existe). */
    fun observeProfile(uid: String): Flow<UserProfile?>

    /** Crea el doc si no existe; si existe, no toca nada. */
    suspend fun ensureProfile(uid: String, email: String?, seed: ProfileInput)

    /** Upsert parcial (merge). Devuelve la representación de dominio. */
    suspend fun upsertProfile(uid: String, email: String?, input: ProfileInput): UserProfile
}