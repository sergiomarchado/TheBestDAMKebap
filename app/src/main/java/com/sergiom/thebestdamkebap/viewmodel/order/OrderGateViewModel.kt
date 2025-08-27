package com.sergiom.thebestdamkebap.viewmodel.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sergiom.thebestdamkebap.domain.order.OrderContext
import com.sergiom.thebestdamkebap.domain.order.OrderMode
import com.sergiom.thebestdamkebap.domain.order.OrderSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel puente para el **gate** de pedido.
 *
 * Responsabilidades:
 * - Exponer el [OrderContext] observable desde [OrderSessionRepository].
 * - Orquestar acciones de sesión: empezar pedido, “solo mirando”, limpiar.
 *
 * Notas:
 * - Sin estado propio: delega en el repositorio (única fuente de verdad).
 */
@HiltViewModel
class OrderGateViewModel @Inject constructor(
    private val session: OrderSessionRepository
) : ViewModel() {

    /** Contexto observable del pedido. Siempre emite el último valor conocido. */
    val context: StateFlow<OrderContext> = session.context

    /** Confirma el inicio/actualización de un pedido con [mode] y [addressId] (opcional). */
    fun confirmStart(mode: OrderMode, addressId: String?) {
        viewModelScope.launch { session.startOrder(mode, addressId) }
    }

    /** Marca el modo “solo estoy mirando” (no obliga a elegir nada). */
    fun chooseBrowsing() {
        viewModelScope.launch { session.setBrowsingOnly() }
    }

    /** Limpia cualquier dato de sesión (p. ej., tras logout). */
    fun clear() {
        viewModelScope.launch { session.clear() }
    }
}
