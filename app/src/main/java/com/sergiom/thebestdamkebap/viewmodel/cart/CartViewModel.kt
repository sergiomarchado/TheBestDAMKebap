package com.sergiom.thebestdamkebap.viewmodel.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sergiom.thebestdamkebap.domain.address.AddressRepository
import com.sergiom.thebestdamkebap.domain.auth.AuthRepository
import com.sergiom.thebestdamkebap.domain.cart.CartRepository
import com.sergiom.thebestdamkebap.domain.order.OrderContext
import com.sergiom.thebestdamkebap.domain.order.OrderMode
import com.sergiom.thebestdamkebap.domain.order.OrderSessionRepository
import com.sergiom.thebestdamkebap.domain.orders.OrdersRepository
import com.sergiom.thebestdamkebap.domain.profile.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cart: CartRepository,
    private val orders: OrdersRepository,
    private val session: OrderSessionRepository,
    private val auth: AuthRepository,
    private val profiles: ProfileRepository,
    private val addresses: AddressRepository
) : ViewModel() {

    val state = cart.state
    val orderCtx: StateFlow<OrderContext> = session.context

    val totalItems: StateFlow<Int> =
        cart.state.map { it.totalItems }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val placeMutex = Mutex()
    private val _placing = MutableStateFlow(false)
    val placing: StateFlow<Boolean> = _placing

    sealed interface Event {
        data class Success(val orderId: String) : Event
        data class Error(val message: String) : Event
    }
    private val _events = MutableSharedFlow<Event>()
    val events: SharedFlow<Event> = _events.asSharedFlow()

    /* ───────── Líneas ───────── */
    fun inc(lineId: String) = viewModelScope.launch {
        val curr = state.value.items.firstOrNull { it.lineId == lineId } ?: return@launch
        cart.updateQuantity(lineId, curr.qty + 1)
    }
    fun dec(lineId: String) = viewModelScope.launch {
        val curr = state.value.items.firstOrNull { it.lineId == lineId } ?: return@launch
        val next = curr.qty - 1
        if (next <= 0) cart.remove(lineId) else cart.updateQuantity(lineId, next)
    }
    fun remove(lineId: String) = viewModelScope.launch { cart.remove(lineId) }
    fun clear() = viewModelScope.launch { cart.clear() }

    /* ───────── Modo/Dirección ───────── */
    fun chooseMode(mode: OrderMode) = viewModelScope.launch {
        val current = session.context.value
        val address = if (mode == OrderMode.PICKUP) null else current.addressId
        session.startOrder(mode, address)
        if (mode == OrderMode.DELIVERY) reconcileAddressIfNeeded()
    }

    /** Si falta address en DELIVERY, intenta poner la del perfil o la primera. */
    fun reconcileAddressIfNeeded() = viewModelScope.launch {
        val ctx = session.context.value
        if (ctx.mode != OrderMode.DELIVERY || !ctx.addressId.isNullOrBlank()) return@launch

        val uid = auth.currentUser.firstOrNull()?.id ?: return@launch

        val defId = profiles.observeProfile(uid).first()?.defaultAddressId
        val candidate: String? = when {
            !defId.isNullOrBlank() && addresses.observeAddress(uid, defId).first() != null -> defId
            else -> addresses.observeAddresses(uid).first().firstOrNull()?.id
        }

        if (!candidate.isNullOrBlank()) {
            session.startOrder(OrderMode.DELIVERY, candidate)
        }
    }

    /* ───────── Checkout ───────── */
    fun checkout() = viewModelScope.launch {
        placeMutex.withLock {
            if (_placing.value) return@withLock
            _placing.value = true
            try {
                val cartState = state.value
                if (cartState.items.isEmpty()) {
                    _events.emit(Event.Error("El carrito está vacío"))
                    return@withLock
                }

                val ctx = session.context.value

                // 1) Modo no nulo
                val mode: OrderMode = ctx.mode ?: run {
                    _events.emit(Event.Error("Selecciona modo de pedido (Recogida o Envío)."))
                    return@withLock
                }

                // 2) DELIVERY → revalidar y corregir addressId contra Firestore (estado actual)
                var finalAddressId: String? = null
                if (mode == OrderMode.DELIVERY) {
                    val uid = auth.currentUser.firstOrNull()?.id
                    if (uid == null) {
                        _events.emit(Event.Error("Debes iniciar sesión para completar el pedido."))
                        return@withLock
                    }

                    // Lista real de direcciones del usuario
                    val list = addresses.observeAddresses(uid).first()

                    // (a) si el id del contexto sigue existiendo, usarlo
                    val currentId = ctx.addressId
                    finalAddressId = currentId?.takeIf { id -> list.any { it.id == id } }

                    // (b) si no, intentar defaultAddressId del perfil (si sigue viva)
                    if (finalAddressId == null) {
                        val defId = profiles.observeProfile(uid).first()?.defaultAddressId
                        if (!defId.isNullOrBlank() && list.any { it.id == defId }) {
                            finalAddressId = defId
                        }
                    }

                    // (c) si no, primera de la lista
                    if (finalAddressId == null) {
                        finalAddressId = list.firstOrNull()?.id
                    }

                    if (finalAddressId == null) {
                        _events.emit(Event.Error("No tienes una dirección válida seleccionada. Añade o elige una."))
                        return@withLock
                    }

                    // Si corregimos el id, sincroniza el OrderSession para el resto de la app
                    if (finalAddressId != currentId) {
                        // Sincroniza la OrderSession para que la UI vea el id corregido.
                        session.startOrder(OrderMode.DELIVERY, finalAddressId)
                        // No hace falta volver a leer ctx.
                    }
                }

                // 3) Crear pedido (para PICKUP, finalAddressId es null por contrato)
                val orderId = orders.submit(cartState, mode, finalAddressId)
                cart.clear()
                _events.emit(Event.Success(orderId))

            } catch (t: Throwable) {
                val msg =
                    if (t.message?.contains("PERMISSION_DENIED", true) == true)
                        "No se pudo crear el pedido. Revisa el modo seleccionado y la dirección de entrega."
                    else t.message ?: "Error al crear el pedido"
                _events.emit(Event.Error(msg))
            } finally {
                _placing.value = false
            }
        }
    }
}
