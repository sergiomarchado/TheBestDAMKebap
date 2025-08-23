// data/address/FirebaseAddressRepository.kt
package com.sergiom.thebestdamkebap.data.address

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import com.sergiom.thebestdamkebap.di.ApplicationScope
import com.sergiom.thebestdamkebap.domain.address.AddressRepository
import com.sergiom.thebestdamkebap.domain.address.Address as DomainAddress
import com.sergiom.thebestdamkebap.domain.address.AddressInput as DomainAddressInput
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
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementación Firebase/Firestore de [AddressRepository].
 *
 * - Flows compartidos con caché por recurso.
 * - Mapea Firestore → modelo de dominio [DomainAddress].
 * - Escrituras por merge y transacciones para operaciones atómicas.
 */
@Singleton
class FirebaseAddressRepository @Inject constructor(
    private val db: FirebaseFirestore,
    @param:ApplicationScope private val appScope: CoroutineScope
) : AddressRepository {

    private fun userDoc(uid: String) = db.collection("users").document(uid)
    private fun addrCol(uid: String) = userDoc(uid).collection("addresses")

    /** Cache por UID para no abrir varios listeners de **lista** del mismo usuario. */
    private val sharedLists = ConcurrentHashMap<String, StateFlow<List<DomainAddress>>>()
    /** Cache por (UID,AID) para lecturas **granulares** de una única dirección. */
    private val sharedById = ConcurrentHashMap<String, StateFlow<DomainAddress?>>()

    /** Observa todas las direcciones de un usuario. */
    override fun observeAddresses(uid: String): Flow<List<DomainAddress>> =
        sharedLists.computeIfAbsent(uid) { _: String ->
            addrCol(uid)
                .snapshots()
                .map { qs ->
                    // 1) Leemos DTOs de data, 2) orden estable por createdAt del DTO, 3) mapeamos a dominio
                    qs.documents
                        .map { d ->
                            val dto = d.toObject(Address::class.java) ?: Address()
                            dto.copy(id = d.id)
                        }
                        .sortedBy { it.createdAt?.time ?: Long.MIN_VALUE }
                        .map { it.toDomain() }
                }
                .distinctUntilChanged()
                .stateIn(appScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

    /** Observa una dirección concreta. */
    override fun observeAddress(uid: String, aid: String): Flow<DomainAddress?> {
        val key = "$uid/$aid"
        return sharedById.computeIfAbsent(key) { _: String ->
            addrCol(uid).document(aid)
                .snapshots()
                .map { snap ->
                    if (!snap.exists()) null
                    else {
                        val dto = snap.toObject(Address::class.java) ?: Address()
                        dto.copy(id = snap.id).toDomain()
                    }
                }
                .distinctUntilChanged()
                .stateIn(appScope, SharingStarted.WhileSubscribed(5_000), null)
        }
    }

    /**
     * Crea/actualiza una dirección (merge).
     * - Si [aid] es `null` ⇒ crea doc nuevo y devuelve su id.
     * - Si [aid] tiene valor ⇒ upsert sobre ese documento.
     */
    override suspend fun upsertAddress(uid: String, aid: String?, input: DomainAddressInput): String {
        val ref = if (aid.isNullOrBlank()) addrCol(uid).document() else addrCol(uid).document(aid)

        // ⚠️ Tipos explícitos: mezcla String/Double/FieldValue ⇒ Any?
        val data = buildMap<String, Any?> {
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

    /** Borra una dirección; si era la predeterminada, limpia `defaultAddressId`. */
    override suspend fun deleteAddress(uid: String, aid: String) {
        val userRef = userDoc(uid)
        val addrRef = addrCol(uid).document(aid)

        db.runTransaction { tx ->
            val userSnap = tx.get(userRef)
            val currentDefault = userSnap.getString("defaultAddressId")
            if (currentDefault == aid) {
                tx.update(
                    userRef,
                    mapOf(
                        "defaultAddressId" to FieldValue.delete(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            tx.delete(addrRef)
        }.await()
    }

    /** Marca una dirección como predeterminada, verificando que exista. */
    override suspend fun setDefaultAddress(uid: String, aid: String) {
        val userRef = userDoc(uid)
        val addrRef = addrCol(uid).document(aid)

        db.runTransaction { tx ->
            val addrSnap = tx.get(addrRef)
            if (!addrSnap.exists()) throw IllegalArgumentException("La dirección no existe")
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
