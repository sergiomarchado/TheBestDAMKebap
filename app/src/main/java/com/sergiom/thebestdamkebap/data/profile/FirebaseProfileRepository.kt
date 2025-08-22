package com.sergiom.thebestdamkebap.data.profile

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Date
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseProfileRepository @Inject constructor(
    private val db: FirebaseFirestore
) : ProfileRepository {

    private fun usersDoc(uid: String) = db.collection("users").document(uid)

    override fun observeProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val reg = usersDoc(uid).addSnapshotListener { snap, err ->
            if (err != null) {
                // Si hay error, emitimos null; también podrías cerrar el flow.
                trySend(null)
                return@addSnapshotListener
            }
            val profile = snap?.toObject(UserProfile::class.java)
            trySend(profile)
        }
        awaitClose { reg.remove() }
    }

    override suspend fun upsertProfile(uid: String, email: String?, input: ProfileInput): UserProfile {
        val data = hashMapOf<String, Any?>(
            "uid" to uid,
            "email" to email, // denormalización (puede ser null si Auth aún no lo tiene)
            "givenName" to input.givenName,
            "familyName" to input.familyName,
            "phone" to input.phone,
            "birthDate" to input.birthDateMillis?.let { Date(it) },
            "updatedAt" to FieldValue.serverTimestamp(),
            // Si el doc no existe, `set(..., merge)` creará y respetará createdAt si lo ponemos:
            "createdAt" to FieldValue.serverTimestamp()
        )
        // Merge: actualiza lo que venga y mantiene lo no enviado.
        usersDoc(uid).set(data, SetOptions.merge()).await()
        // Lectura consistente (no estrictamente necesaria si UI recibe el snapshot):
        return usersDoc(uid).get().await().toObject(UserProfile::class.java)
            ?: UserProfile(uid = uid, email = email)
    }
}

/* -------- Helpers Task.await() locales (idéntico patrón a tu AuthViewModel) -------- */
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
