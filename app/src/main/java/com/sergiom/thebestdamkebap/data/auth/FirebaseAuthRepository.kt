package com.sergiom.thebestdamkebap.data.auth

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.sergiom.thebestdamkebap.domain.auth.AuthRepository
import com.sergiom.thebestdamkebap.domain.auth.DomainUser
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Implementación de [AuthRepository] sobre FirebaseAuth.
 *
 * Responsabilidad:
 * - Exponer un API de dominio (Flow/suspend) y mapear [FirebaseUser] → [DomainUser].
 * - No decide UX (p. ej., bloquear no verificados); eso queda en el ViewModel.
 */
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    init {
        // Los emails de verificación / reset usan el idioma del dispositivo.
        // (Si prefieres forzar español: auth.setLanguageCode("es"))
        auth.useAppLanguage()
    }

    /**
     * Flujo reactivo del usuario actual.
     * Se basa en AuthStateListener y emite `DomainUser?` en cada cambio.
     */
    override val currentUser: Flow<DomainUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { fa ->
            trySend(fa.currentUser?.toDomain())
        }
        auth.addAuthStateListener(listener)

        // Emisión inicial por si el listener tarda en dispararse
        trySend(auth.currentUser?.toDomain())

        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /** Crea sesión anónima solo si no hay usuario. */
    override suspend fun signInAnonymouslyIfNeeded() {
        if (auth.currentUser != null) return
        auth.signInAnonymously().await()
    }

    /** Login con email y contraseña; devuelve el usuario resultante. */
    override suspend fun signInWithEmail(email: String, password: String): DomainUser {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val user = result.user ?: error("No se pudo iniciar sesión.")
        return user.toDomain()
    }

    /**
     * Registro con email/contraseña.
     *
     * Si el usuario actual es anónimo, se hace **link** para conservar UID/datos.
     * En caso contrario, se crea la cuenta y se inicia sesión con ella.
     * Si `name` no es nulo/vacío, se actualiza `displayName`.
     */
    override suspend fun registerWithEmail(
        name: String?,
        email: String,
        password: String
    ): DomainUser {
        val current = auth.currentUser
        val credential = EmailAuthProvider.getCredential(email, password)

        val user: FirebaseUser? = if (current != null && current.isAnonymous) {
            current.linkWithCredential(credential).await().user
        } else {
            auth.createUserWithEmailAndPassword(email, password).await().user
        }

        if (!name.isNullOrBlank() && user != null) {
            val req = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            user.updateProfile(req).await()
        }

        return user?.toDomain() ?: error("No se pudo registrar usuario.")
    }

    /** Envía correo de restablecimiento de contraseña. */
    override suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    /** Cierra la sesión actual. */
    override suspend fun signOut() {
        auth.signOut()
    }

    /** Envía correo de verificación al usuario actual (si lo hay). */
    override suspend fun requestEmailVerification() {
        auth.currentUser?.sendEmailVerification()?.await()
    }
}

/* ======================= Mapper Firebase → Dominio ======================= */

/** Convierte [FirebaseUser] a [DomainUser] para no filtrar Firebase a capas superiores. */
private fun FirebaseUser.toDomain(): DomainUser =
    DomainUser(
        id = uid,
        name = displayName,
        email = email,
        isAnonymous = isAnonymous,
        isEmailVerified = isEmailVerified
    )
