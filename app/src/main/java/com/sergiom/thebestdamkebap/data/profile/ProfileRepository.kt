// data/profile/ProfileRepository.kt
package com.sergiom.thebestdamkebap.data.profile

import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfile(uid: String): Flow<UserProfile?>

    /** Crea el doc si no existe (pone createdAt); si existe, no toca createdAt. */
    suspend fun ensureProfile(uid: String, email: String?, seed: ProfileInput)

    /** Actualiza campos vía merge (idempotente); NO toca createdAt. */
    suspend fun upsertProfile(uid: String, email: String?, input: ProfileInput): UserProfile
}
