package com.sergiom.thebestdamkebap.viewmodel.home
/**
 * Estado inmutable de la pantalla **Home**.
 *
 * Contrato:
 * - Representa **estado persistente de UI** (no efímero). Los eventos one-shot (snackbars,
 *   navegación) van en [HomeEvent] y se emiten por `SharedFlow` desde el ViewModel.
 * - Es **inmutable** y se expone como `StateFlow<UiState>` para Compose.
 *
 * Campos:
 * @property isLoading Indica si hay una carga/refresh en curso. Útil para deshabilitar UI y
 *                     mostrar indicadores (p. ej., pull-to-refresh/progress en top bar).
 * @property cartCount Contador visible del carrito (temporal hasta conectar repo/UseCase).
 *
 * Extensiones futuras (sugerencias):
 * - Datos de contenido: `offers: List<OfferUi>`, `categories: List<CategoryUi>`, etc.
 * - Estado de errores **no efímeros** (para mostrar placeholders): `errorMessageResId: Int?`
 *   o un `ContentState` sellado (Empty/Failure/Data).
 * - Filtros/paginación/ordenación: `query: String`, `selectedCategoryId: String?`, `page: Int`, …
 * - UI hints: `isRefreshing: Boolean` si además de `isLoading` distingues acciones del usuario.
 */
data class UiState(
    val isLoading: Boolean = false,
    val cartCount: Int = 0,
)
