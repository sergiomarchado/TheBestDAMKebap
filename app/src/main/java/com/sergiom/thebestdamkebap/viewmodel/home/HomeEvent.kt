package com.sergiom.thebestdamkebap.viewmodel.home
/**
 * Eventos efímeros que el ViewModel de **Home** emite hacia la UI.
 *
 * Propósito:
 * - Comunicar sucesos **one-shot** (snackbars, navegación) que **no** forman parte del
 *   estado persistente de la pantalla (ese va en [UiState]).
 *
 * Uso recomendado:
 * - En el ViewModel, expón `SharedFlow<HomeEvent>` y emite con `tryEmit()`/`emit`.
 * - En la UI (Compose), colecciona dentro de `LaunchedEffect(Unit)` usando `collectLatest`
 *   para mostrar snackbars y lanzar navegación sin re-emitir en recomposición.
 *
 * Internacionalización:
 * - Los textos `text` deberían resolverse desde `strings.xml` en la capa que emite/consume.
 *
 * Extensiones futuras (ejemplos):
 * - `NavigateToProduct(val id: String)`
 * - `ShowAddToCartConfirmation(val productName: String)`
 * - `OpenExternalUrl(val url: String)`
 */
sealed interface HomeEvent {
    data class Error(val text: String) : HomeEvent
    data class Info(val text: String)  : HomeEvent
    object NavigateToCart : HomeEvent
}