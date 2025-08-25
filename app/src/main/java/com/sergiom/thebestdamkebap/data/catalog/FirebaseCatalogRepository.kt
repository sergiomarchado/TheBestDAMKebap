package com.sergiom.thebestdamkebap.data.catalog

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.sergiom.thebestdamkebap.domain.catalog.CatalogRepository
import com.sergiom.thebestdamkebap.domain.catalog.Category
import com.sergiom.thebestdamkebap.domain.catalog.Prices
import com.sergiom.thebestdamkebap.domain.catalog.Product
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

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
                // Podrías enviar emptyList o cerrar; enviamos vacío para no crashear la UI.
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snap?.documents?.mapNotNull { d ->
                val name = d.getString("name") ?: return@mapNotNull null
                val order = d.getLong("order") ?: 0L
                val active = d.getBoolean("active") ?: false
                val imagePath = d.getString("imagePath")
                Category(
                    id = d.id,
                    name = name,
                    order = order,
                    active = active,
                    imagePath = imagePath
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
}
