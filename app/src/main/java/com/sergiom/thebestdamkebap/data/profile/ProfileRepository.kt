package com.sergiom.thebestdamkebap.data.profile

import kotlinx.coroutines.flow.Flow

/**
 * Acceso al perfil del usuario en Firestore.
 * No gestiona credenciales (email/password) → eso lo hace FirebaseAuth.
 */
interface ProfileRepository {
    /** Observa en tiempo real el documento `/users/{uid}`. */
    fun observeProfile(uid: String): Flow<UserProfile?>

    /**
     * Crea el documento si no existe (con campos básicos) o actualiza campos vía merge.
     * Devuelve el perfil tras la operación (lectura consistente).
     */
    suspend fun upsertProfile(uid: String, email: String?, input: ProfileInput): UserProfile
}
