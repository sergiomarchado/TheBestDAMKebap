package com.sergiom.thebestdamkebap.viewmodel.home
/**
 * Eventos de **una sola vez** que el [HomeViewModel] comunica a la UI.
 *
 * Diferencia con el estado ([UiState]):
 * - El estado representa "cómo está la pantalla ahora".
 * - Los eventos representan sucesos puntuales: mostrar un mensaje o lanzar una navegación.
 *   No deben guardarse en el estado porque solo tienen que ocurrir **una vez**.
 *
 * Cómo se usan:
 * - El ViewModel emite estos eventos en un `SharedFlow<HomeEvent>`.
 * - La UI (Compose) los recibe dentro de `LaunchedEffect(Unit)` usando `collectLatest`.
 *   Así se asegura de ejecutarlos sin re-dispararlos en cada recomposición.
 *
 * Notas:
 * - Los textos (`text`) deberían obtenerse de `strings.xml` para soportar varios idiomas.
 * - Se pueden añadir más tipos de evento según vaya creciendo la pantalla
 *   (ej. navegación a producto, mostrar confirmaciones, abrir enlaces externos).
 */
sealed interface HomeEvent {
    data class Error(val text: String) : HomeEvent
    data class Info(val text: String)  : HomeEvent
    object NavigateToCart : HomeEvent
}