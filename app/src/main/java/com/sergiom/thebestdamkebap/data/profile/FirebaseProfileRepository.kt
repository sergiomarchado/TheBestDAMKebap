// data/profile/FirebaseProfileRepository.kt
package com.sergiom.thebestdamkebap.data.profile

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import com.sergiom.thebestdamkebap.di.ApplicationScope
import com.sergiom.thebestdamkebap.domain.profile.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Repositorio de **perfil de usuario** en Firestore.
 *
 * Reglas que respeta:
 * - `createdAt` solo se fija en **create** (no se toca en updates).
 * - `updatedAt` se actualiza en cada escritura (salvo en `ensureProfile` cuando ya existe).
 * - Solo escribe claves válidas según reglas de seguridad (uid/email/givenName/...).
 *
 * Diseño:
 * - `observeProfile(uid)` comparte listener por UID usando un `StateFlow` cacheado en `appScope`.
 * - Escrituras idempotentes mediante `SetOptions.merge()` (no sobrescribe con `null`).
 */
class FirebaseProfileRepository @Inject constructor(
    private val db: FirebaseFirestore,
    @param:ApplicationScope private val appScope: CoroutineScope
) : ProfileRepository {

    private fun usersDoc(uid: String) = db.collection("users").document(uid)

    /** Cache por UID para no abrir varios listeners del mismo doc. */
    private val sharedByUid = ConcurrentHashMap<String, StateFlow<UserProfile?>>()

    override fun observeProfile(uid: String): Flow<UserProfile?> =
        sharedByUid.getOrPut(uid) {
            usersDoc(uid)
                .snapshots() // Flow<DocumentSnapshot>
                .map { snap -> if (snap.exists()) snap.toObject(UserProfile::class.java) else null }
                .distinctUntilChanged()
                .stateIn(appScope, SharingStarted.WhileSubscribed(5_000), null)
        }

    /**
     * Crea el doc si no existe. Si existe **no toca nada** (ni `updatedAt`).
     * - Fija `uid`, `email` (si se pasa), campos del seed (si no son vacíos),
     *   y `createdAt/updatedAt = serverTimestamp()` **solo en creación**.
     */
    override suspend fun ensureProfile(uid: String, email: String?, seed: ProfileInput) {
        val doc = usersDoc(uid)
        db.runTransaction { tx ->
            val snap = tx.get(doc)
            if (!snap.exists()) {
                val data = buildMap {
                    put("uid", uid)
                    email?.takeIf { it.isNotBlank() }?.let { put("email", it) }
                    seed.givenName?.takeIf { it.isNotBlank() }?.let { put("givenName", it) }
                    seed.familyName?.takeIf { it.isNotBlank() }?.let { put("familyName", it) }
                    seed.phone?.takeIf { it.isNotBlank() }?.let { put("phone", it) }
                    seed.birthDateMillis?.let { put("birthDate", Date(it)) } // Firestore lo guarda como Timestamp
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                // Al ser alta, podemos usar set() simple; merge también es válido.
                tx.set(doc, data, SetOptions.merge())
            }
            // Si existe → no modificar (contrato de ensureProfile)
            null
        }.await()
    }

    /**
     * Upsert parcial (merge) sin leer después; la UI se actualizará por el snapshot.
     * - Mantiene `createdAt` intacto.
     * - Actualiza `updatedAt = serverTimestamp()`.
     * - Ignora valores nulos/vacíos (no sobrescribe con null).
     */
    override suspend fun upsertProfile(uid: String, email: String?, input: ProfileInput): UserProfile {
        val data = buildMap {
            put("uid", uid)
            email?.takeIf { it.isNotBlank() }?.let { put("email", it) }
            input.givenName?.takeIf { it.isNotBlank() }?.let { put("givenName", it) }
            input.familyName?.takeIf { it.isNotBlank() }?.let { put("familyName", it) }
            input.phone?.takeIf { it.isNotBlank() }?.let { put("phone", it) }
            input.birthDateMillis?.let { put("birthDate", Date(it)) }
            put("updatedAt", FieldValue.serverTimestamp())
        }
        usersDoc(uid).set(data, SetOptions.merge()).await()

        // Devolvemos una representación local; el Flow emitirá luego con los timestamps reales.
        return UserProfile(
            uid = uid,
            email = email,
            givenName = input.givenName,
            familyName = input.familyName,
            phone = input.phone,
            birthDate = input.birthDateMillis?.let { Date(it) }
        )
    }
}

/* await() local para Tasks (si prefieres, puedes usar kotlinx-coroutines-play-services). */
private suspend fun <T> Task<T>.await(): T =
    suspendCancellableCoroutine { cont ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (cont.isActive) cont.resume(task.result)
            } else {
                val ex = task.exception ?: RuntimeException("Error desconocido en Firebase Task")
                if (cont.isActive) cont.resumeWithException(ex)
            }
        }
    }
