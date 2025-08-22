// data/profile/FirebaseProfileRepository.kt
package com.sergiom.thebestdamkebap.data.profile

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import com.sergiom.thebestdamkebap.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
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

    private val sharedByUid = ConcurrentHashMap<String, StateFlow<UserProfile?>>()

    override fun observeProfile(uid: String): Flow<UserProfile?> {
        return sharedByUid.getOrPut(uid) {
            usersDoc(uid)
                .snapshots()
                .map { snap -> if (snap.exists()) snap.toObject(UserProfile::class.java) else null }
                .distinctUntilChanged()
                .stateIn(
                    scope = appScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = null
                )
        }
    }

    override suspend fun ensureProfile(uid: String, email: String?, seed: ProfileInput) {
        val doc = usersDoc(uid)
        db.runTransaction { tx ->
            val snap = tx.get(doc)
            val data = mutableMapOf(
                "uid" to uid,
                "email" to email,
                "givenName" to seed.givenName,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!snap.exists()) {
                data["createdAt"] = FieldValue.serverTimestamp()
            }
            tx.set(doc, data, SetOptions.merge())
        }.await()
    }

    override suspend fun upsertProfile(uid: String, email: String?, input: ProfileInput): UserProfile {
        val data = mutableMapOf(
            "uid" to uid,
            "email" to email,
            "givenName" to input.givenName,
            "familyName" to input.familyName,
            "phone" to input.phone,
            "birthDate" to input.birthDateMillis?.let { Date(it) },
            "updatedAt" to FieldValue.serverTimestamp()
            // ⚠️ sin createdAt aquí
        )
        usersDoc(uid).set(data, SetOptions.merge()).await()

        // Representación local (el Flow emitirá el snapshot real después)
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
