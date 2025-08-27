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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val DS_NAME = "order_session"

// DataStore atado al ApplicationContext
private val Context.orderDataStore: DataStore<Preferences> by preferencesDataStore(DS_NAME)

/**
 * Implementación basada en **DataStore (Preferences)** con caché en memoria.
 *
 * Propiedades:
 * - @Singleton: vida de app; expone un único [StateFlow] con el contexto actual.
 * - Lectura inicial (hidratación) desde DataStore al crear el repositorio.
 * - Escrituras atómicas (memoria + persistencia) protegidas con [Mutex].
 */
@Singleton
class OrderSessionRepositoryImpl @Inject constructor(
    @ApplicationContext private val appContext: Context
) : OrderSessionRepository {

    private object Keys {
        val MODE = stringPreferencesKey("mode")          // "DELIVERY" | "PICKUP"
        val ADDRESS_ID = stringPreferencesKey("address") // nullable
        val BROWSING = booleanPreferencesKey("browsing") // true → “solo mirando”
    }

    // Scope propio del repo (vida de app)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Serializa las escrituras para mantener consistencia entre memoria y DataStore.
    private val writeMutex = Mutex()

    // Último contexto conocido en memoria (observable por la UI).
    private val inMemory = MutableStateFlow(OrderContext())

    init {
        // Hidratación inicial desde DataStore; maneja errores con vacío por seguridad.
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

    override val context: StateFlow<OrderContext> = inMemory

    override suspend fun startOrder(mode: OrderMode, addressId: String?) {
        writeMutex.withLock {
            // 1) Actualiza memoria (UI responde inmediatamente).
            inMemory.value = OrderContext(mode = mode, addressId = addressId, browsingOnly = false)
            // 2) Persiste de forma transaccional en DataStore.
            appContext.orderDataStore.edit { e ->
                e[Keys.MODE] = mode.name
                if (addressId == null) e.remove(Keys.ADDRESS_ID) else e[Keys.ADDRESS_ID] = addressId
                e[Keys.BROWSING] = false
            }
        }
    }

    override suspend fun setBrowsingOnly() {
        writeMutex.withLock {
            inMemory.value = OrderContext(browsingOnly = true)
            appContext.orderDataStore.edit { e ->
                e.remove(Keys.MODE)
                e.remove(Keys.ADDRESS_ID)
                e[Keys.BROWSING] = true
            }
        }
    }

    override suspend fun clear() {
        writeMutex.withLock {
            inMemory.value = OrderContext()
            appContext.orderDataStore.edit { it.clear() }
        }
    }
}
