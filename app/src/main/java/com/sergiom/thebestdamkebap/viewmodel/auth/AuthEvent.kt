package com.sergiom.thebestdamkebap.viewmodel.auth
/**
 * Eventos efímeros de autenticación para comunicar sucesos **one-shot** desde la capa
 * de ViewModel hacia la UI (Compose).
 *
 * Uso recomendado:
 * - Emite con `SharedFlow<AuthEvent>` (p. ej., `tryEmit(...)`) desde el ViewModel.
 * - Consume en UI con `LaunchedEffect(Unit) { events.collectLatest { ... } }` y muestra
 *   snackbars/navegación correspondientes **una sola vez** (no es estado persistente).
 *
 * Notas:
 * - Diferenciar **Error** (fallos que requieren atención) de **Info** (mensajes neutrales).
 * - Para navegación, usar objetos (`object`) sin payload cuando no hace falta dato extra.
 * - Los textos de usuario deberían venir de `strings.xml` en la capa que emite/consume.
 *
 */
sealed interface AuthEvent {
    data class Error(val text: String) : AuthEvent
    data class Info(val text: String)  : AuthEvent
    object RegisterSuccess : AuthEvent
    object NavigateToLogin : AuthEvent
}