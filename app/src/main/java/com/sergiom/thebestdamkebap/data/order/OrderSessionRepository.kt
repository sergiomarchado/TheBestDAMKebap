package com.sergiom.thebestdamkebap.data.order

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sergiom.thebestdamkebap.domain.order.OrderContext
import com.sergiom.thebestdamkebap.domain.order.OrderMode
import com.sergiom.thebestdamkebap.domain.order.OrderSessionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val DS_NAME = "order_session"

// DataStore atado al ApplicationContext
private val Context.orderDataStore: DataStore<Preferences> by preferencesDataStore(DS_NAME)

@Singleton
class OrderSessionRepositoryImpl @Inject constructor(
    @ApplicationContext private val appContext: Context   // ⬅️ renombrado
) : OrderSessionRepository {

    private object Keys {
        val MODE = stringPreferencesKey("mode")          // "DELIVERY" | "PICKUP"
        val ADDRESS_ID = stringPreferencesKey("address") // nullable
        val BROWSING = booleanPreferencesKey("browsing") // true → “solo mirando”
    }

    // Scope propio del repo (vida de app por ser @Singleton)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val inMemory = MutableStateFlow(OrderContext())

    init {
        // Hidratar memoria desde DataStore una vez al crear el repo
        scope.launch {
            val initial = appContext.orderDataStore.data
                .catch { emit(emptyPreferences()) }
                .map { prefs ->
                    val mode = prefs[Keys.MODE]?.let { runCatching { OrderMode.valueOf(it) }.getOrNull() }
                    val addr = prefs[Keys.ADDRESS_ID]
                    val browsing = prefs[Keys.BROWSING] ?: false
                    OrderContext(mode = mode, addressId = addr, browsingOnly = browsing)
                }
                .first()
            inMemory.value = initial
        }
    }

    // ← Esta propiedad viene del interface; ahora no colisiona con el Context de Android
    override val context: StateFlow<OrderContext> = inMemory

    override suspend fun startOrder(mode: OrderMode, addressId: String?) {
        inMemory.value = OrderContext(mode = mode, addressId = addressId, browsingOnly = false)
        appContext.orderDataStore.edit { e ->
            e[Keys.MODE] = mode.name
            if (addressId == null) e.remove(Keys.ADDRESS_ID) else e[Keys.ADDRESS_ID] = addressId
            e[Keys.BROWSING] = false
        }
    }

    override suspend fun setBrowsingOnly() {
        inMemory.value = OrderContext(browsingOnly = true)
        appContext.orderDataStore.edit { e ->
            e.remove(Keys.MODE)
            e.remove(Keys.ADDRESS_ID)
            e[Keys.BROWSING] = true
        }
    }

    override suspend fun clear() {
        inMemory.value = OrderContext()
        appContext.orderDataStore.edit { it.clear() }
    }
}
