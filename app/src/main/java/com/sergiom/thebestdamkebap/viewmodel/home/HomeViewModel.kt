package com.sergiom.thebestdamkebap.viewmodel.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    // TODO: Inyecta repos aquí cuando estén listos:
    // private val offersRepo: OffersRepository,
    // private val productsRepo: ProductsRepository,
    // private val cartRepo: CartRepository,
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val cartCount: Int = 0,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    init {
        // Ejemplo: cuando tengas repo de carrito:
        // viewModelScope.launch {
        //   cartRepo.cartCountFlow.collect { count -> _ui.update { it.copy(cartCount = count) } }
        // }
    }

    fun refresh() {
        if (_ui.value.isLoading) return
        _ui.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                // TODO cargar contenido inicial (ofertas/categorías…)
                _events.tryEmit(HomeEvent.Info("Contenido actualizado"))
            } catch (_: Throwable) {
                _events.tryEmit(HomeEvent.Error("No se pudo actualizar el contenido"))
            } finally {
                _ui.update { it.copy(isLoading = false) }
            }
        }
    }

    fun openCart() { _events.tryEmit(HomeEvent.NavigateToCart) }

    // Utilidades temporales mientras no hay repo de carrito:
    fun setCartCount(v: Int) { _ui.update { it.copy(cartCount = v.coerceAtLeast(0)) } }
    fun incCart() { _ui.update { it.copy(cartCount = it.cartCount + 1) } }
    fun decCart() { _ui.update { it.copy(cartCount = (it.cartCount - 1).coerceAtLeast(0)) } }
}

sealed interface HomeEvent {
    data class Error(val text: String) : HomeEvent
    data class Info(val text: String)  : HomeEvent
    object NavigateToCart : HomeEvent
}
