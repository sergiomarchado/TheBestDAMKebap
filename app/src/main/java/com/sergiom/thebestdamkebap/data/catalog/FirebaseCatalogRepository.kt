package com.sergiom.thebestdamkebap.data.catalog

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.sergiom.thebestdamkebap.domain.catalog.CatalogRepository
import com.sergiom.thebestdamkebap.domain.catalog.Category
import com.sergiom.thebestdamkebap.domain.catalog.CategoryType
import com.sergiom.thebestdamkebap.domain.catalog.Prices
import com.sergiom.thebestdamkebap.domain.catalog.Product
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.Normalizer

@Singleton
class FirebaseCatalogRepository @Inject constructor(
    private val db: FirebaseFirestore
) : CatalogRepository {

    override fun observeCategories(): Flow<List<Category>> = callbackFlow {
        val q = db.collection("categories")
            .whereEqualTo("active", true)
            .orderBy("order")

        val reg: ListenerRegistration = q.addSnapshotListener { snap, err ->
            if (err != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            val list = snap?.documents?.mapNotNull { d ->
                val name = d.getString("name") ?: return@mapNotNull null
                val order = d.getLong("order") ?: 0L
                val active = d.getBoolean("active") ?: false
                val imagePath = d.getString("imagePath")

                // type: tolerante a acentos y caso
                val typeStr = d.getString("type")?.let {
                    Normalizer.normalize(it, Normalizer.Form.NFD)
                        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                        .uppercase()
                }
                val type = when (typeStr) {
                    "MENUS" -> CategoryType.MENUS
                    else    -> CategoryType.PRODUCTS
                }

                Category(
                    id = d.id,
                    name = name,
                    order = order,
                    active = active,
                    imagePath = imagePath,
                    type = type
                )
            }.orEmpty()

            trySend(list)
        }

        awaitClose { reg.remove() }
    }

    override fun observeProducts(categoryId: String?): Flow<List<Product>> = callbackFlow {
        var q = db.collection("products")
            .whereEqualTo("active", true)

        if (categoryId != null) {
            q = q.whereEqualTo("categoryId", categoryId)
        }
        q = q.orderBy("order")

        val reg = q.addSnapshotListener { snap, err ->
            if (err != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            val list = snap?.documents?.mapNotNull { d ->
                val name = d.getString("name") ?: return@mapNotNull null
                val order = d.getLong("order") ?: 0L
                val active = d.getBoolean("active") ?: false
                val category = d.getString("categoryId") ?: return@mapNotNull null
                val description = d.getString("description")
                val imagePath = d.getString("imagePath")
                val ingredients = (d.get("ingredients") as? List<*>)?.filterIsInstance<String>().orEmpty()

                val pricesMap = d.get("prices") as? Map<*, *>
                val prices = Prices(
                    pickup = (pricesMap?.get("pickup") as? Number)?.toLong(),
                    delivery = (pricesMap?.get("delivery") as? Number)?.toLong()
                )

                Product(
                    id = d.id,
                    name = name,
                    description = description,
                    imagePath = imagePath,
                    categoryId = category,
                    active = active,
                    order = order,
                    ingredients = ingredients,
                    prices = prices
                )
            }.orEmpty()

            trySend(list)
        }

        awaitClose { reg.remove() }
    }

    // Para el builder de menús: lectura puntual por ids (maneja límite de whereIn=10)
    override suspend fun getProductsByIds(ids: List<String>): List<Product> {
        if (ids.isEmpty()) return emptyList()
        val distinct = ids.distinct()
        val chunks = distinct.chunked(10)

        val all = mutableListOf<Product>()
        for (batch in chunks) {
            val snap = db.collection("products")
                .whereIn(FieldPath.documentId(), batch)
                .get()
                .await()

            val mapped = snap.documents.mapNotNull { d ->
                val name = d.getString("name") ?: return@mapNotNull null
                val order = d.getLong("order") ?: 0L
                val active = d.getBoolean("active") ?: false
                val category = d.getString("categoryId") ?: return@mapNotNull null
                val description = d.getString("description")
                val imagePath = d.getString("imagePath")
                val ingredients = (d.get("ingredients") as? List<*>)?.filterIsInstance<String>().orEmpty()

                val pricesMap = d.get("prices") as? Map<*, *>
                val prices = Prices(
                    pickup = (pricesMap?.get("pickup") as? Number)?.toLong(),
                    delivery = (pricesMap?.get("delivery") as? Number)?.toLong()
                )

                Product(
                    id = d.id,
                    name = name,
                    description = description,
                    imagePath = imagePath,
                    categoryId = category,
                    active = active,
                    order = order,
                    ingredients = ingredients,
                    prices = prices
                )
            }
            all += mapped
        }

        // Opcional: conservar orden según ids de entrada
        val orderIndex = distinct.withIndex().associate { it.value to it.index }
        return all.sortedBy { orderIndex[it.id] ?: Int.MAX_VALUE }
    }
}
