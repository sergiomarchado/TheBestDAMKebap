// com/sergiom/thebestdamkebap/viewmodel/cart/CartViewModel.kt
package com.sergiom.thebestdamkebap.viewmodel.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sergiom.thebestdamkebap.domain.cart.CartRepository
import com.sergiom.thebestdamkebap.domain.catalog.CatalogRepository
import com.sergiom.thebestdamkebap.domain.catalog.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cart: CartRepository,
    private val catalog: CatalogRepository
) : ViewModel() {

    val state = cart.state

    // Contador para el badge del FAB
    val totalItems: StateFlow<Int> =
        cart.state.map { it.totalItems }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // ===== Resolver productId -> Product (para mostrar nombres/mini resúmenes)
    private val _productMap = MutableStateFlow<Map<String, Product>>(emptyMap())
    val productMap: StateFlow<Map<String, Product>> = _productMap

    fun resolveProducts(ids: Set<String>) = viewModelScope.launch {
        if (ids.isEmpty()) { _productMap.value = emptyMap(); return@launch }
        val list = catalog.getProductsByIds(ids.toList())
        _productMap.value = list.associateBy { it.id }
    }

    // ===== Helpers de la pantalla
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
}
