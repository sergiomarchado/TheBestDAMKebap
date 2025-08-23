package com.sergiom.thebestdamkebap.viewmodel.auth
/**
 * AuthEvent
 *
 * Eventos efímeros de autenticación para comunicar sucesos **one-shot** desde el ViewModel
 * hacia la UI (Compose). No forman parte del estado persistente; la pantalla los consume
 * una sola vez (snackbar, navegación, etc.).
 *
 * Cómo usar:
 * - ViewModel: emitir por un `SharedFlow<AuthEvent>` (p. ej., `_events.tryEmit(...)`).
 * - UI (Compose): coleccionar en `LaunchedEffect(Unit)` con `collectLatest { ... }`
 *   y realizar la acción correspondiente (mostrar mensaje, navegar…).
 *
 * Recomendaciones:
 * - Los textos visibles deberían resolverse desde `strings.xml` en el punto de emisión o consumo.
 * - Mantener los eventos de navegación como `object` cuando no requieren datos extra.
 */
sealed interface AuthEvent {
    /** Mensaje de error para mostrar al usuario (snackbar/diálogo). */
    data class Error(val text: String) : AuthEvent

    /** Mensaje informativo (confirmaciones, avisos no críticos). */
    data class Info(val text: String)  : AuthEvent

    /**
     * Registro completado con éxito.
     * Suele encadenarse con: solicitar verificación → cerrar sesión → ir a Login.
     */
    object RegisterSuccess : AuthEvent

    /**
     * Orden explícita de navegar a Login (p. ej., tras enviar verificación y cerrar sesión).
     * No incluye payload; la UI decide cómo navegar.
     */
    object NavigateToLogin : AuthEvent
}