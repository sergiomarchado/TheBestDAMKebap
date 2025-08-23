// data/profile/ProfileRepository.kt
package com.sergiom.thebestdamkebap.data.profile

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

    /**
     * Observa en tiempo real el perfil del usuario.
     *
     * @param uid ID del usuario (y del documento `/users/{uid}`).
     * @return `Flow<UserProfile?>` que emite:
     *   - `null` si el documento no existe.
     *   - Un `UserProfile` cada vez que el documento cambia.
     *
     * Sugerencia de uso en UI:
     * - Colecciona con `collectAsStateWithLifecycle()` para respetar el ciclo de vida.
     */
    fun observeProfile(uid: String): Flow<UserProfile?>

    /**
     * Garantiza que exista el documento de perfil.
     *
     * Qué hace:
     * - Si **no existe**, lo crea con:
     *   - `uid` (doc id), `email` (si se pasa), y los campos de [seed].
     *   - `createdAt = serverTimestamp()`, `updatedAt = serverTimestamp()`.
     * - Si **ya existe**, **no** hace cambios (ni siquiera `updatedAt`).
     *
     * @param uid   ID del usuario (doc `/users/{uid}`).
     * @param email Email conocido (opcional; si es `null`, se ignora).
     * @param seed  Datos iniciales (nombre, etc.). Los `null` se ignoran.
     */
    suspend fun ensureProfile(uid: String, email: String?, seed: ProfileInput)

    /**
     * Actualiza el perfil de forma idempotente (merge).
     *
     * Qué hace:
     * - Escribe **solo** los campos no nulos de [input] y/o `email` si no es `null`.
     * - Mantiene `createdAt` intacto.
     * - Actualiza `updatedAt = serverTimestamp()`.
     *
     * @return El `UserProfile` resultante tras la escritura (si la implementación
     *         hace un `get()`/read-back) o el último conocido.
     *
     * Recomendación:
     * - Evitar enviar `null` para “borrar” campos desde aquí; si necesitas borrados,
     *   expón un método específico (p. ej. `clearPhone(uid)`).
     */
    suspend fun upsertProfile(uid: String, email: String?, input: ProfileInput): UserProfile
}
