// data/address/FirebaseAddressRepository.kt
package com.sergiom.thebestdamkebap.data.address

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObject
import com.sergiom.thebestdamkebap.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseAddressRepository @Inject constructor(
    private val db: FirebaseFirestore,
    @param:ApplicationScope private val appScope: CoroutineScope
) : AddressRepository {

    private fun userDoc(uid: String) = db.collection("users").document(uid)
    private fun addrCol(uid: String) = userDoc(uid).collection("addresses")

    /** Cache por UID para no abrir varios listeners de lista. */
    private val sharedLists = ConcurrentHashMap<String, StateFlow<List<Address>>>()
    /** Cache por (UID,AID) para lecturas finas. */
    private val sharedById = ConcurrentHashMap<String, StateFlow<Address?>>()

    override fun observeAddresses(uid: String): Flow<List<Address>> =
        sharedLists.getOrPut(uid) {
            addrCol(uid)
                .snapshots()
                .map { qs ->
                    qs.documents.map { doc ->
                        (doc.toObject<Address>() ?: Address()).copy(id = doc.id)
                    }
                }
                .distinctUntilChanged()
                .stateIn(appScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

    override fun observeAddress(uid: String, aid: String): Flow<Address?> {
        val key = "$uid/$aid"
        return sharedById.getOrPut(key) {
            addrCol(uid).document(aid)
                .snapshots()
                .map { snap ->
                    if (!snap.exists()) null
                    else (snap.toObject<Address>() ?: Address()).copy(id = snap.id)
                }
                .distinctUntilChanged()
                .stateIn(appScope, SharingStarted.WhileSubscribed(5_000), null)
        }
    }

    override suspend fun upsertAddress(uid: String, aid: String?, input: AddressInput): String {
        val ref = if (aid.isNullOrBlank()) addrCol(uid).document() else addrCol(uid).document(aid)

        // Solo campos no nulos (coincide con las reglas de validación)
        val data = buildMap {
            input.label?.let { put("label", it) }
            input.recipientName?.let { put("recipientName", it) }
            input.phone?.let { put("phone", it) }
            input.street?.let { put("street", it) }
            input.number?.let { put("number", it) }
            input.floorDoor?.let { put("floorDoor", it) }
            input.city?.let { put("city", it) }
            input.province?.let { put("province", it) }
            input.postalCode?.let { put("postalCode", it) }
            input.notes?.let { put("notes", it) }
            input.lat?.let { put("lat", it) }
            input.lng?.let { put("lng", it) }
            put("updatedAt", FieldValue.serverTimestamp())
            if (aid.isNullOrBlank()) put("createdAt", FieldValue.serverTimestamp())
        }

        ref.set(data, SetOptions.merge()).await()
        return ref.id
    }

    override suspend fun deleteAddress(uid: String, aid: String) {
        val userRef = userDoc(uid)
        val addrRef = addrCol(uid).document(aid)

        // Transacción: borra address y limpia defaultAddressId si apuntaba a ella
        db.runTransaction { tx ->
            val userSnap = tx.get(userRef)
            val currentDefault = userSnap.getString("defaultAddressId")
            if (currentDefault == aid) {
                tx.update(userRef, mapOf("defaultAddressId" to FieldValue.delete(), "updatedAt" to FieldValue.serverTimestamp()))
            }
            tx.delete(addrRef)
        }.await()
    }

    override suspend fun setDefaultAddress(uid: String, aid: String) {
        val userRef = userDoc(uid)
        val addrRef = addrCol(uid).document(aid)

        // Transacción: verifica existencia de la dirección y marca default en /users/{uid}
        db.runTransaction { tx ->
            val addrSnap = tx.get(addrRef)
            if (!addrSnap.exists()) {
                throw IllegalArgumentException("La dirección no existe")
            }
            tx.set(
                userRef,
                mapOf(
                    "defaultAddressId" to aid,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
        }.await()
    }
}

/* await helper */
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
