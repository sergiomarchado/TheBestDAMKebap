// viewmodel/orders/OrdersViewModel.kt
package com.sergiom.thebestdamkebap.viewmodel.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sergiom.thebestdamkebap.domain.auth.AuthRepository
import com.sergiom.thebestdamkebap.domain.cart.CartRepository
import com.sergiom.thebestdamkebap.domain.cart.addFromReorderLines
import com.sergiom.thebestdamkebap.domain.catalog.CatalogRepository
import com.sergiom.thebestdamkebap.domain.menu.MenuRepository
import com.sergiom.thebestdamkebap.domain.orders.OrderSummary
import com.sergiom.thebestdamkebap.domain.orders.OrdersRepository
import com.sergiom.thebestdamkebap.domain.orders.ReorderLine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val orders: OrdersRepository,
    private val cart: CartRepository,
    private val catalog: CatalogRepository,   // productos
    private val menus: MenuRepository         // menús (observeMenu)
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val list: List<OrderSummary> = emptyList(),
        val error: String? = null,
        val isGuest: Boolean = true,
        val info: String? = null
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            auth.currentUser
                .distinctUntilChanged { a, b -> a?.id == b?.id }
                .flatMapLatest { u ->
                    if (u == null || u.isAnonymous) {
                        _ui.update { it.copy(loading = false, list = emptyList(), isGuest = true, error = null) }
                        emptyFlow()
                    } else {
                        _ui.update { it.copy(loading = true, isGuest = false, error = null) }
                        orders.observeMyOrders(u.id, limit = 20)
                    }
                }
                .catch { e -> _ui.update { it.copy(loading = false, error = e.message ?: "No se pudieron cargar los pedidos") } }
                .collect { list -> _ui.update { it.copy(loading = false, list = list, error = null) } }
        }
    }

    /** Repite un pedido: carga productos/menús por id y los añade al carrito con sus personalizaciones. */
    fun repeatOrder(lines: List<ReorderLine>) = viewModelScope.launch {
        try {
            cart.addFromReorderLines(
                lines = lines,
                loadProductById = { id ->
                    // CatalogRepository: usamos la puntual por ids y cogemos el primero
                    catalog.getProductsByIds(listOf(id)).firstOrNull()
                },
                loadMenuById = { id ->
                    // MenuRepository: solo tenemos observeMenu → tomamos el primer valor disponible
                    menus.observeMenu(id).firstOrNull()
                }
            )
            _ui.update { it.copy(info = "Añadido al carrito") }
        } catch (t: Throwable) {
            _ui.update { it.copy(error = t.message ?: "No se pudo repetir el pedido") }
        }
    }

    fun consumeInfo() { _ui.update { it.copy(info = null) } }
}
