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

/**
 * Implementación Firebase/Firestore de [AddressRepository].
 *
 * Responsabilidades:
 * - Exponer **flows fríos** (hot-shared internamente) con los snapshots de Firestore.
 * - Mapear `DocumentSnapshot` → [Address], inyectando `doc.id` en `Address.id`.
 * - Ejecutar escrituras con **merge** (upsert) y gestionar transacciones para
 *   operaciones que afectan a varias rutas (p. ej., borrar + limpiar `defaultAddressId`).
 *
 * Caching de listeners:
 * - Para evitar múltiples listeners del mismo doc/colección, se cachea cada Flow
 *   con `stateIn(appScope, WhileSubscribed)` dentro de un `ConcurrentHashMap`.
 * - Esto mantiene un único listener por UID o (UID,AID) a nivel de proceso.
 *   Si prevés *muchos* UID/AID distintos en un mismo proceso, valora política de limpieza.
 *
 * Notas:
 * - Las validaciones (CP, teléfono, etc.) están en el **UseCase** de dominio.
 * - Los mensajes de usuario se gestionan en VM/UI; aquí retornamos excepciones “técnicas”.
 */
class FirebaseAddressRepository @Inject constructor(
    private val db: FirebaseFirestore,
    @param:ApplicationScope private val appScope: CoroutineScope
) : AddressRepository {

    private fun userDoc(uid: String) = db.collection("users").document(uid)
    private fun addrCol(uid: String) = userDoc(uid).collection("addresses")

    /** Cache por UID para no abrir varios listeners de **lista** del mismo usuario. */
    private val sharedLists = ConcurrentHashMap<String, StateFlow<List<Address>>>()
    /** Cache por (UID,AID) para lecturas **granulares** de una única dirección. */
    private val sharedById = ConcurrentHashMap<String, StateFlow<Address?>>()

    /**
     * Observa todas las direcciones del usuario en `/users/{uid}/addresses`.
     * - Emite **lista completa** en cada snapshot.
     * - Cada `Address` incluye `id = doc.id`.
     */
    override fun observeAddresses(uid: String): Flow<List<Address>> =
        sharedLists.getOrPut(uid) {
            addrCol(uid)
                .snapshots()
                .map { qs ->
                    qs.documents.map { doc ->
                        // `toObject` puede devolver null si falta el schema; usamos defaults.
                        (doc.toObject<Address>() ?: Address()).copy(id = doc.id)
                    }
                }
                .distinctUntilChanged()
                .stateIn(appScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

    /**
     * Observa una **dirección concreta**.
     * - Emite `null` si el doc no existe (p. ej., eliminado desde otro cliente).
     */
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

    /**
     * Crea/actualiza una dirección (merge).
     * - Si [aid] es `null` ⇒ crea doc **nuevo** y devuelve su `id`.
     * - Si [aid] tiene valor ⇒ hace **merge** sobre el doc existente.
     * - Escribe `createdAt` solo en alta y `updatedAt` siempre (serverTimestamp).
     * - **No** toca `defaultAddressId`; eso lo gestiona [setDefaultAddress].
     */
    override suspend fun upsertAddress(uid: String, aid: String?, input: AddressInput): String {
        val ref = if (aid.isNullOrBlank()) addrCol(uid).document() else addrCol(uid).document(aid)

        // Solo campos no nulos (coincide con las reglas de validación/UseCase)
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

    /**
     * Borra una dirección del usuario.
     * - Si la dirección borrada es la **predeterminada**, limpia `defaultAddressId` en `/users/{uid}`.
     * - Se hace en **transacción** para garantizar atomicidad.
     */
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

    /**
     * Marca como **predeterminada** una dirección existente.
     * - Verifica que el doc `/users/{uid}/addresses/{aid}` existe.
     * - Actualiza `defaultAddressId` en `/users/{uid}` (merge) con `updatedAt`.
     * - Se hace en **transacción** para evitar condiciones de carrera.
     *
     * @throws IllegalArgumentException si la dirección no existe.
     */
    override suspend fun setDefaultAddress(uid: String, aid: String) {
        val userRef = userDoc(uid)
        val addrRef = addrCol(uid).document(aid)

        db.runTransaction { tx ->
            val addrSnap = tx.get(addrRef)
            if (!addrSnap.exists()) {
                // Importante: fallar dentro de la transacción para abortarla.
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

/* await helper — convierte Task<T> en suspensión segura con cancelación */
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
