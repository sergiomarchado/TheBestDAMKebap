// data/profile/FirebaseProfileRepository.kt
package com.sergiom.thebestdamkebap.data.profile

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import com.sergiom.thebestdamkebap.di.ApplicationScope
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
                .snapshots()
                .map { snap -> if (snap.exists()) snap.toObject(UserProfile::class.java) else null }
                .distinctUntilChanged()
                .stateIn(appScope, SharingStarted.WhileSubscribed(5_000), null)
        }

    /** Crea (si no existe) o garantiza el doc de perfil con un seed mínimo. */
    override suspend fun ensureProfile(uid: String, email: String?, seed: ProfileInput) {
        val doc = usersDoc(uid)
        db.runTransaction { tx ->
            val snap = tx.get(doc)
            val data = buildMap {
                put("uid", uid)
                email?.let { put("email", it) }
                seed.givenName?.let { put("givenName", it) }
                seed.familyName?.let { put("familyName", it) }
                seed.phone?.let { put("phone", it) }
                seed.birthDateMillis?.let { put("birthDate", Date(it)) }
                put("updatedAt", FieldValue.serverTimestamp())
                if (!snap.exists()) put("createdAt", FieldValue.serverTimestamp())
            }
            tx.set(doc, data, SetOptions.merge())
        }.await()
    }

    /** Upsert parcial (merge) sin leer después; la UI se actualizará por el snapshot. */
    override suspend fun upsertProfile(uid: String, email: String?, input: ProfileInput): UserProfile {
        val data = buildMap {
            put("uid", uid)
            email?.let { put("email", it) }
            input.givenName?.let { put("givenName", it) }
            input.familyName?.let { put("familyName", it) }
            input.phone?.let { put("phone", it) }
            input.birthDateMillis?.let { put("birthDate", Date(it)) }
            put("updatedAt", FieldValue.serverTimestamp())
        }
        usersDoc(uid).set(data, SetOptions.merge()).await()

        // Devolvemos una representación local; el Flow emitirá el valor real (timestamps) luego.
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

/* await() */
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
