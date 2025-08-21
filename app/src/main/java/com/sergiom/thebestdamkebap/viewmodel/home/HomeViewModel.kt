package com.sergiom.thebestdamkebap.viewmodel.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
/**
 * HomeViewModel
 *
 * Responsabilidad:
 * - Orquestar el estado de la pantalla de Home ([UiState]) y emitir eventos efímeros
 *   ([HomeEvent]) hacia la UI (snackbars/navegación).
 * - Exponer operaciones de UI como `refresh()` y accesos rápidos de carrito (mientras no hay repos).
 *
 * Colaboradores (futuros):
 * - Repositorios / casos de uso (Offers, Products, Cart) inyectados con Hilt.
 *   El VM **no** debería conocer detalles de red/DB; sólo invoca UseCases/Repos y mapea a UI.
 *
 * Estado:
 * - [_ui]: `StateFlow<UiState>` inmutable para la UI (colectado con collectAsStateWithLifecycle).
 * - [_events]: `SharedFlow<HomeEvent>` para one-shots (snackbars/nav). Buffer 1 para evitar pérdidas.
 *
 * Concurrencia:
 * - `refresh()` evita solaparse revisando `isLoading`. Si un repo expone Flows “siempre vivos”,
 *   convendrá observarlos y derivar `UiState` con `stateIn`, en vez de “refrescar” manualmente.
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


