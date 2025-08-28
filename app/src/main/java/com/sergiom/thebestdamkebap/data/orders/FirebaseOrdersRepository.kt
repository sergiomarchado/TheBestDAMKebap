// data/orders/FirebaseOrdersRepository.kt
package com.sergiom.thebestdamkebap.data.orders

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.sergiom.thebestdamkebap.domain.address.AddressSnap
import com.sergiom.thebestdamkebap.domain.cart.CartItem
import com.sergiom.thebestdamkebap.domain.cart.CartState
import com.sergiom.thebestdamkebap.domain.cart.MenuLine
import com.sergiom.thebestdamkebap.domain.cart.ProductLine
import com.sergiom.thebestdamkebap.domain.order.OrderMode
import com.sergiom.thebestdamkebap.domain.orders.OrderLinePreview
import com.sergiom.thebestdamkebap.domain.orders.OrderSummary
import com.sergiom.thebestdamkebap.domain.orders.ReorderLine
import com.sergiom.thebestdamkebap.domain.orders.OrdersRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Singleton
class FirebaseOrdersRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : OrdersRepository {

    private companion object { const val TAG = "OrdersRepo" }

    /* ---------- CREATE (con logs detallados) ---------- */

    override suspend fun submit(
        cart: CartState,
        mode: OrderMode,
        addressId: String?,
        deliveryAddress: AddressSnap?
    ): String {
        val uid = auth.currentUser?.uid
            ?: error("Debes iniciar sesión para completar el pedido.")

        Log.d(TAG, "submit(): uid=$uid, mode=$mode, addressId=$addressId, items=${cart.items.size}, total=${cart.totalCents}")

        val items = cart.items.map { it.toFirestoreItem() }

        @Suppress("RemoveRedundantCallsOfConversionMethods")
        val doc = mutableMapOf<String, Any?>(
            "userId" to uid,
            "items" to items,
            "total" to cart.totalCents.toLong(),
            "status" to "PENDING",
            "createdAt" to FieldValue.serverTimestamp(),
            "mode" to mode.name
        )

        if (mode == OrderMode.DELIVERY) {
            require(!addressId.isNullOrBlank()) { "Para envío a domicilio debes elegir una dirección." }
            requireNotNull(deliveryAddress) { "Falta snapshot de la dirección de entrega." }

            // Chequeo rápido para log
            quickCheckDeliverySnap(deliveryAddress)?.let { warn ->
                Log.w(TAG, "deliveryAddress quick-check warning: $warn ; snap=${deliveryAddress.toMap()}")
            }

            doc["addressId"] = addressId
            doc["deliveryAddress"] = deliveryAddress.toMap() // ⬅️ ahora sin claves nulas
        }

        // Para el log, hacemos legible el serverTimestamp
        val debugDoc = doc.toMutableMap().apply { this["createdAt"] = "serverTimestamp()" }
        Log.d(TAG, "About to write order. keys=${doc.keys} payload=$debugDoc")

        val ref = db.collection("orders").document()
        try {
            ref.set(doc).await()
            Log.d(TAG, "Order created OK: id=${ref.id}")
            return ref.id
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "Order create FAILED: code=${e.code}, msg=${e.message}, payload=$debugDoc", e)
            throw e
        } catch (t: Throwable) {
            Log.e(TAG, "Order create FAILED (other): ${t.message}, payload=$debugDoc", t)
            throw t
        }
    }

    /* ---------- READ: últimos pedidos con detalles ---------- */

    override fun observeMyOrders(uid: String, limit: Int): Flow<List<OrderSummary>> {
        return db.collection("orders")
            .whereEqualTo("userId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .snapshots()
            .map { qs ->
                qs.documents.map { d ->
                    @Suppress("UNCHECKED_CAST")
                    val rawItems: List<Map<String, Any?>> =
                        (d.get("items") as? List<*>)?.mapNotNull { it as? Map<String, Any?> } ?: emptyList()

                    val itemsCount = rawItems.sumOf { m -> (m["qty"] as? Number)?.toInt() ?: 0 }

                    val previews: List<OrderLinePreview> = rawItems.map { m ->
                        val type = (m["type"] as? String).orEmpty()
                        val qty  = (m["qty"]  as? Number)?.toInt() ?: 1
                        val name = (m["name"] as? String)
                            ?: (m["productId"] as? String)
                            ?: (m["menuId"] as? String)
                            ?: "Artículo"

                        val text = if (type == "menu") {
                            val selMap = (m["selections"] as? Map<*, *>).orEmpty()
                            val detail = selMap.entries.joinToString(" · ") { (group, list) ->
                                val entries = (list as? List<*>)?.mapNotNull { it as? Map<*, *> }.orEmpty()
                                val inner = entries.joinToString(", ") { sel ->
                                    val pid = sel["productId"] as? String ?: "?"
                                    val removed = (sel["removedIngredients"] as? List<*>)?.filterIsInstance<String>().orEmpty()
                                    if (removed.isEmpty()) pid else "$pid (sin ${removed.joinToString()})"
                                }
                                "${group.toString()}: $inner"
                            }
                            "$name — $detail"
                        } else {
                            val removed = (m["removedIngredients"] as? List<*>)?.filterIsInstance<String>().orEmpty()
                            if (removed.isEmpty()) name else "$name (sin ${removed.joinToString()})"
                        }

                        OrderLinePreview(qty = qty, text = text)
                    }

                    val reorderLines: List<ReorderLine> = rawItems.mapNotNull { m ->
                        when (m["type"]) {
                            "product" -> ReorderLine.Product(
                                productId = m["productId"] as? String ?: return@mapNotNull null,
                                name = m["name"] as? String,
                                imagePath = m["imagePath"] as? String,
                                unitPriceCents = (m["unitPriceCents"] as? Number)?.toInt() ?: 0,
                                qty = (m["qty"] as? Number)?.toInt() ?: 1,
                                removedIngredients = (m["removedIngredients"] as? List<*>)?.filterIsInstance<String>().orEmpty()
                            )
                            "menu" -> {
                                @Suppress("UNCHECKED_CAST")
                                val selMap = (m["selections"] as? Map<String, List<Map<String, Any?>>>).orEmpty()
                                val selections = selMap.mapValues { (_, list) ->
                                    list.map { s ->
                                        ReorderLine.Menu.Selection(
                                            productId = s["productId"] as? String ?: "",
                                            removedIngredients = (s["removedIngredients"] as? List<*>)?.filterIsInstance<String>().orEmpty()
                                        )
                                    }
                                }
                                ReorderLine.Menu(
                                    menuId = m["menuId"] as? String ?: return@mapNotNull null,
                                    name = m["name"] as? String,
                                    imagePath = m["imagePath"] as? String,
                                    unitPriceCents = (m["unitPriceCents"] as? Number)?.toInt() ?: 0,
                                    qty = (m["qty"] as? Number)?.toInt() ?: 1,
                                    selections = selections
                                )
                            }
                            else -> null
                        }
                    }

                    OrderSummary(
                        id = d.id,
                        createdAtMillis = d.getTimestamp("createdAt")?.toDate()?.time,
                        status = d.getString("status") ?: "PENDING",
                        totalCents = d.getLong("total") ?: 0L,
                        mode = runCatching { OrderMode.valueOf(d.getString("mode") ?: "PICKUP") }.getOrDefault(OrderMode.PICKUP),
                        addressId = d.getString("addressId"),
                        itemsCount = itemsCount,
                        previews = previews,
                        reorderLines = reorderLines
                    )
                }
            }
            .catch { e -> throw e }
    }

    /* ---------- mappers ---------- */

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

    // ⬇️ CLAVE: no incluir claves con null; lat/lng solo si ambos != null
    private fun AddressSnap.toMap(): Map<String, Any?> = buildMap {
        label?.let { put("label", it) }
        recipientName?.let { put("recipientName", it) }
        put("phone", phone) // requerido por reglas
        put("street", street)
        put("number", number)
        floorDoor?.let { put("floorDoor", it) }
        put("city", city)
        province?.let { put("province", it) }
        put("postalCode", postalCode)
        notes?.let { put("notes", it) }
        if (lat != null && lng != null) {
            put("lat", lat)
            put("lng", lng)
        }
    }

    /** Chequeo rápido para logs: refleja las reglas más críticas. */
    private fun quickCheckDeliverySnap(s: AddressSnap): String? {
        val phoneOk = Regex("^\\+?[0-9]{9,15}$").matches(s.phone)
        if (!phoneOk) return "phone inválido (${s.phone})"
        val cpOk = Regex("^\\d{5}$").matches(s.postalCode)
        if (!cpOk) return "postalCode inválido (${s.postalCode})"
        val latLngOk = (s.lat == null && s.lng == null) || (s.lat != null && s.lng != null)
        if (!latLngOk) return "lat/lng incongruentes (lat=${s.lat}, lng=${s.lng})"
        return null
    }
}
