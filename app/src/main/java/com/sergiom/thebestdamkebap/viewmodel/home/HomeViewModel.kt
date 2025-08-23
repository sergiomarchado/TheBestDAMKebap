package com.sergiom.thebestdamkebap.viewmodel.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
/**
 * ViewModel de la pantalla Home.
 *
 * Qué hace:
 * - Mantiene el estado que la pantalla necesita (contador del carrito, cargando, etc.).
 * - Lanza avisos puntuales para la UI (snackbars o navegación), sin repetirlos en recomposiciones.
 * - Expone acciones sencillas como `refresh()` u “abrir carrito”.
 *
 * Cómo encaja en la app:
 * - Más adelante recibirá repositorios o casos de uso por inyección (ofertas, productos, carrito).
 *   El ViewModel no accede a red/BD directamente: solo coordina y traduce a un estado de UI.
 *
 * Estado expuesto:
 * - [ui]: flujo con el estado actual de la pantalla para que Compose lo observe.
 * - [events]: flujo de eventos efímeros (no se guardan en el estado).
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    // TODO: Inyecta repos aquí cuando estén listos:
    // private val offersRepo: OffersRepository,
    // private val productsRepo: ProductsRepository,
    // private val cartRepo: CartRepository,
) : ViewModel() {

    // Estado principal de UI (inmutable hacia fuera).
    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    // Eventos efímeros: snackbars, navegación a cart, etc.
    private val _events = MutableSharedFlow<HomeEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    init {
        // Ejemplo (cuando tengas repos/casos de uso de flujo continuo):
        // viewModelScope.launch {
        //   observeCartCount() // Flow<Int>
        //     .collect { count -> _ui.update { it.copy(cartCount = count) } }
        // }
        //
        // NOTE: Para cargar contenido inicial, puedes llamar a `refresh()` aquí
        //       o exponer un UseCase que emita Flows y derivar UiState con stateIn.
    }

    /**
     * Refresca contenido de Home (ofertas/categorías/etc.).
     *
     * Política actual:
     * - Evita refrescos simultáneos usando `isLoading`.
     * - Emite `HomeEvent.Info/Error` según resultado.
     *
     * TODO (cuando haya casos de uso):
     * - Invocar `getHomeContent()` y mapear éxito/errores a UiState y eventos.
     * - Considerar un “result wrapper” (Success/Error) para mensajes de usuario coherentes.
     */
    fun refresh() {
        if (_ui.value.isLoading) return  // evita solapamiento de llamadas
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

    /** Solicita a la UI navegar al carrito. */
    fun openCart() { _events.tryEmit(HomeEvent.NavigateToCart) }

    // ─────────────────────────────────────────────────────────────────────────────
    // Utilidades temporales mientras no hay repo de carrito:
    // ─────────────────────────────────────────────────────────────────────────────
    /** Setea el contador del carrito (temporal, mientras no hay repos/casos de uso). */
    fun setCartCount(v: Int) { _ui.update { it.copy(cartCount = v.coerceAtLeast(0)) } }

    /** Incrementa el contador del carrito (temporal). */
    fun incCart() { _ui.update { it.copy(cartCount = it.cartCount + 1) } }

    /** Decrementa el contador del carrito (temporal, no baja de 0). */
    fun decCart() { _ui.update { it.copy(cartCount = (it.cartCount - 1).coerceAtLeast(0)) } }
}


