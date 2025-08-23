package com.sergiom.thebestdamkebap.data.profile

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import com.sergiom.thebestdamkebap.di.ApplicationScope
import com.sergiom.thebestdamkebap.domain.profile.ProfileRepository
import com.sergiom.thebestdamkebap.domain.profile.UserProfile as DomainUserProfile
import com.sergiom.thebestdamkebap.domain.profile.ProfileInput as DomainProfileInput
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

/**
 * Repositorio de perfil de usuario en Firestore (devuelve modelos de dominio).
 */
@Singleton
class FirebaseProfileRepository @Inject constructor(
    private val db: FirebaseFirestore,
    @param:ApplicationScope private val appScope: CoroutineScope
) : ProfileRepository {

    private fun usersDoc(uid: String) = db.collection("users").document(uid)

    /** Cache por UID para no abrir varios listeners del mismo doc. */
    private val sharedByUid = ConcurrentHashMap<String, StateFlow<DomainUserProfile?>>()

    override fun observeProfile(uid: String): Flow<DomainUserProfile?> =
        sharedByUid.computeIfAbsent(uid) {
            usersDoc(uid)
                .snapshots()
                .map { snap ->
                    if (!snap.exists()) null
                    else (snap.toObject(UserProfile::class.java))?.toDomain()
                }
                .distinctUntilChanged()
                .stateIn(appScope, SharingStarted.WhileSubscribed(5_000), null)
        }

    /** Crea el doc si no existe; si existe, no toca nada. */
    override suspend fun ensureProfile(uid: String, email: String?, seed: DomainProfileInput) {
        val doc = usersDoc(uid)
        db.runTransaction { tx ->
            val snap = tx.get(doc)
            if (!snap.exists()) {
                val data = buildMap<String, Any?> {
                    put("uid", uid)
                    email?.takeIf { it.isNotBlank() }?.let { put("email", it) }
                    seed.givenName?.takeIf { it.isNotBlank() }?.let { put("givenName", it) }
                    seed.familyName?.takeIf { it.isNotBlank() }?.let { put("familyName", it) }
                    seed.phone?.takeIf { it.isNotBlank() }?.let { put("phone", it) }
                    seed.birthDateMillis?.let { put("birthDate", Date(it)) }
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                tx.set(doc, data, SetOptions.merge())
            }
            null
        }.await()
    }

    /** Upsert parcial (merge); mantiene createdAt y actualiza updatedAt. */
    override suspend fun upsertProfile(
        uid: String,
        email: String?,
        input: DomainProfileInput
    ): DomainUserProfile {
        val data = buildMap<String, Any?> {
            put("uid", uid)
            email?.takeIf { it.isNotBlank() }?.let { put("email", it) }
            input.givenName?.takeIf { it.isNotBlank() }?.let { put("givenName", it) }
            input.familyName?.takeIf { it.isNotBlank() }?.let { put("familyName", it) }
            input.phone?.takeIf { it.isNotBlank() }?.let { put("phone", it) }
            input.birthDateMillis?.let { put("birthDate", Date(it)) }
            put("updatedAt", FieldValue.serverTimestamp())
        }
        usersDoc(uid).set(data, SetOptions.merge()).await()

        // Devolvemos dominio local; el snapshot actualizará timestamps reales después.
        return DomainUserProfile(
            uid = uid,
            email = email,
            givenName = input.givenName,
            familyName = input.familyName,
            phone = input.phone,
            birthDateMillis = input.birthDateMillis,
            defaultAddressId = null,        // se mantiene la existente en Firestore
            createdAtMillis = null,         // llegará por el snapshot
            updatedAtMillis = null          // llegará por el snapshot
        )
    }
}
