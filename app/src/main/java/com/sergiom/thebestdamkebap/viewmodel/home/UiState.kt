package com.sergiom.thebestdamkebap.viewmodel.home
/**
 * Estado de la pantalla principal (**Home**).
 *
 * Diferencia con [HomeEvent]:
 * - Aquí guardamos lo que **se mantiene en el tiempo** en la UI.
 * - Los sucesos puntuales (mensajes, navegación) se manejan como eventos aparte.
 *
 * Qué representa:
 * - [isLoading]: si la pantalla está cargando o refrescando contenido.
 *   La UI puede mostrar un spinner o deshabilitar botones mientras sea true.
 * - [cartCount]: número de productos en el carrito (de momento gestionado en memoria).
 *
 * Cómo evoluciona:
 * - El ViewModel expone este estado como `StateFlow<UiState>`.
 * - La UI lo observa con `collectAsStateWithLifecycle()` y se recompone automáticamente
 *   cuando cambian los valores.
 *
 * Crecerá en el futuro con:
 * - Listas de ofertas, categorías, productos, etc.
 * - Estados de error o vacíos (ej. no hay conexión).
 * - Parámetros de búsqueda o filtros.
 */
data class UiState(
    val isLoading: Boolean = false,
    val cartCount: Int = 0,
)
