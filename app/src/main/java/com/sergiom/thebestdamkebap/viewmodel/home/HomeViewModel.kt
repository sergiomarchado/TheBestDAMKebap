package com.sergiom.thebestdamkebap.viewmodel.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel de la pantalla Home.
 *
 * Función:
 * - Mantiene el estado de la UI (carga, contador de carrito, etc.).
 * - Emite eventos de una sola vez (snackbars, navegar al carrito) a través de [events].
 * - Provee acciones simples (`refresh`, `openCart`, …).
 *
 * Diseño:
 * - Exponer `StateFlow<UiState>` inmutable y encapsular el `MutableStateFlow`.
 * - `SharedFlow` para eventos efímeros (no se guardan en estado).
 */
@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    // Estado principal de UI (inmutable hacia fuera).
    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    // Eventos efímeros: snackbars, navegación a cart, etc.
    private val _events = MutableSharedFlow<HomeEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

}


