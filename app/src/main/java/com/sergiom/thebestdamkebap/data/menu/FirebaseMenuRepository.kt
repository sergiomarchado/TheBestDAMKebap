package com.sergiom.thebestdamkebap.data.menu

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.sergiom.thebestdamkebap.domain.catalog.Prices
import com.sergiom.thebestdamkebap.domain.menu.Menu
import com.sergiom.thebestdamkebap.domain.menu.MenuAllowed
import com.sergiom.thebestdamkebap.domain.menu.MenuGroup
import com.sergiom.thebestdamkebap.domain.menu.MenuRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@Singleton
class FirebaseMenuRepository @Inject constructor(
    private val db: FirebaseFirestore
) : MenuRepository {

    override fun observeMenus(): Flow<List<Menu>> = callbackFlow {
        val q = db.collection("menus")
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
                val description = d.getString("description")
                val imagePath = d.getString("imagePath")

                val pricesMap = d.get("prices") as? Map<*, *>
                val prices = Prices(
                    pickup = (pricesMap?.get("pickup") as? Number)?.toLong(),
                    delivery = (pricesMap?.get("delivery") as? Number)?.toLong()
                )

                val groupsRaw = d.get("groups") as? List<*>
                val groups: List<MenuGroup> = groupsRaw?.mapNotNull { g ->
                    (g as? Map<*, *>)?.let { parseGroup(it) }
                }.orEmpty()

                Menu(
                    id = d.id,
                    active = active,
                    name = name,
                    description = description,
                    imagePath = imagePath,
                    order = order,
                    prices = prices,
                    groups = groups
                )
            }.orEmpty()

            trySend(list)
        }

        awaitClose { reg.remove() }
    }

    override fun observeMenu(menuId: String): Flow<Menu?> = callbackFlow {
        val ref = db.collection("menus").document(menuId)

        val reg: ListenerRegistration = ref.addSnapshotListener { snap, err ->
            if (err != null) {
                trySend(null)
                return@addSnapshotListener
            }
            val d = snap ?: run { trySend(null); return@addSnapshotListener }

            if (!d.exists()) {
                trySend(null)
                return@addSnapshotListener
            }

            val name = d.getString("name") ?: run { trySend(null); return@addSnapshotListener }
            val order = d.getLong("order") ?: 0L
            val active = d.getBoolean("active") ?: false
            val description = d.getString("description")
            val imagePath = d.getString("imagePath")

            val pricesMap = d.get("prices") as? Map<*, *>
            val prices = Prices(
                pickup = (pricesMap?.get("pickup") as? Number)?.toLong(),
                delivery = (pricesMap?.get("delivery") as? Number)?.toLong()
            )

            val groupsRaw = d.get("groups") as? List<*>
            val groups: List<MenuGroup> = groupsRaw?.mapNotNull { g ->
                (g as? Map<*, *>)?.let { parseGroup(it) }
            }.orEmpty()

            trySend(
                Menu(
                    id = d.id,
                    active = active,
                    name = name,
                    description = description,
                    imagePath = imagePath,
                    order = order,
                    prices = prices,
                    groups = groups
                )
            )
        }

        awaitClose { reg.remove() }
    }

    // ------- Parsers -------

    private fun parseGroup(m: Map<*, *>): MenuGroup? {
        val id = m["id"] as? String ?: return null
        val name = m["name"] as? String ?: id
        val min = (m["min"] as? Number)?.toInt() ?: 1
        val max = (m["max"] as? Number)?.toInt() ?: 1

        val allowedRaw = m["allowed"] as? List<*>
        val allowed: List<MenuAllowed> = allowedRaw?.mapNotNull { a ->
            (a as? Map<*, *>)?.let { parseAllowed(it) }
        }.orEmpty()

        return MenuGroup(
            id = id,
            name = name,
            min = min,
            max = max,
            allowed = allowed
        )
    }

    private fun parseAllowed(m: Map<*, *>): MenuAllowed? {
        val productId = m["productId"] as? String ?: return null
        val deltaMap = m["delta"] as? Map<*, *>
        val delta = if (deltaMap != null) {
            Prices(
                pickup = (deltaMap["pickup"] as? Number)?.toLong(),
                delivery = (deltaMap["delivery"] as? Number)?.toLong()
            )
        } else null
        val isDefault = (m["default"] as? Boolean) ?: false
        val allowRemoval = (m["allowIngredientRemoval"] as? Boolean) ?: true

        return MenuAllowed(
            productId = productId,
            delta = delta,
            default = isDefault,
            allowIngredientRemoval = allowRemoval
        )
    }
}
