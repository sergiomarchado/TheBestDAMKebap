// data/orders/FirebaseOrdersRepository.kt
package com.sergiom.thebestdamkebap.data.orders

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.sergiom.thebestdamkebap.domain.cart.CartItem
import com.sergiom.thebestdamkebap.domain.cart.CartState
import com.sergiom.thebestdamkebap.domain.cart.MenuLine
import com.sergiom.thebestdamkebap.domain.cart.ProductLine
import com.sergiom.thebestdamkebap.domain.order.OrderMode
import com.sergiom.thebestdamkebap.domain.orders.OrdersRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await

@Singleton
class FirebaseOrdersRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : OrdersRepository {

    private companion object { const val TAG = "OrdersRepo" }

    override suspend fun submit(
        cart: CartState,
        mode: OrderMode,
        addressId: String?
    ): String {
        val uid = auth.currentUser?.uid
            ?: error("Debes iniciar sesión para completar el pedido.")

        // ——— Diagnóstico previo: modo + dirección
        Log.d(TAG, "submit(): uid=$uid, mode=$mode, addressId=$addressId, items=${cart.items.size}, total=${cart.totalCents}")

        // Si es DELIVERY, verificamos que la dirección exista bajo /users/{uid}/addresses/{aid}
        var addressExists = false
        if (mode == OrderMode.DELIVERY) {
            val aid = addressId ?: error("Para envío a domicilio debes elegir una dirección.")
            val addrSnap = db.collection("users").document(uid)
                .collection("addresses").document(aid)
                .get().await()
            addressExists = addrSnap.exists()
            Log.d(TAG, "addressExists($aid) = $addressExists")
            if (!addressExists) {
                error("La dirección seleccionada no es válida. Revísala o elige otra.")
            }
        }

        val items = cart.items.map { it.toFirestoreItem() }

        // Documento a crear (sin addressId si PICKUP)
        val doc = mutableMapOf<String, Any?>(
            "userId" to uid,
            "items" to items,
            "total" to cart.totalCents.toLong(),   // Long para evitar overflow si Cart usa Int
            "status" to "PENDING",
            "createdAt" to FieldValue.serverTimestamp(),
            "mode" to mode.name
        ).apply {
            if (mode == OrderMode.DELIVERY) put("addressId", addressId!!)
        }

        // ——— Más diagnóstico: claves y tipos
        Log.d(TAG, "doc.keys=${doc.keys} (has addressId? ${"addressId" in doc})")
        Log.d(TAG, "items[0] sample=${items.firstOrNull()}")

        // WRITE: create directo con ID aleatorio (sin transacción, sin read previo)
        val ref = db.collection("orders").document()
        try {
            ref.set(doc).await()
            Log.d(TAG, "Order created OK: id=${ref.id}")
            return ref.id
        } catch (t: Throwable) {
            Log.e(TAG, "create failed: ${t.message}", t)
            throw t
        }
    }

    /* ---------- mapeos de líneas ---------- */

    private fun CartItem.toFirestoreItem(): Map<String, Any?> = when (this) {
        is ProductLine -> mapOf(
            "type" to "product",
            "productId" to productId,
            "name" to name,
            "imagePath" to imagePath,
            "unitPriceCents" to unitPriceCents,
            "qty" to qty,
            "subtotalCents" to subtotalCents,
            "removedIngredients" to (customization?.removedIngredients?.toList() ?: emptyList())
        )
        is MenuLine -> mapOf(
            "type" to "menu",
            "menuId" to menuId,
            "name" to name,
            "imagePath" to imagePath,
            "unitPriceCents" to unitPriceCents,
            "qty" to qty,
            "subtotalCents" to subtotalCents,
            // groupId -> [ { productId, removedIngredients[] } ]
            "selections" to selections.mapValues { (_, list) ->
                list.map { s ->
                    mapOf(
                        "productId" to s.productId,
                        "removedIngredients" to (s.customization?.removedIngredients?.toList() ?: emptyList())
                    )
                }
            }
        )
    }
}

/* await utilitario (sin libs extra) */
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) cont.resume(task.result)
        else cont.resumeWithException(task.exception ?: RuntimeException("Firebase Task failed"))
    }
    cont.invokeOnCancellation { /* noop */ }
}
