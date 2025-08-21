package com.sergiom.thebestdamkebap.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sergiom.thebestdamkebap.viewmodel.auth.AuthViewModel

/**
 * Registra la **ruta de Splash** en el grafo raíz.
 *
 * Responsabilidad:
 * - Añadir el destino [Standalone.SPLASH] y delegar la lógica en [SplashRoute].
 *
 * Alcance/DI:
 * - Este destino permite que `hiltViewModel()` resuelva un [AuthViewModel] scopeado al
 *   `NavBackStackEntry` de Splash. Al hacer `popUpTo(SPLASH){ inclusive = true }`, se limpia
 *   también ese scope.
 *
 * @param navController Controlador de navegación compartido por el NavHost.
 */
fun NavGraphBuilder.splashRoute(
    navController: NavHostController
) {
    composable(Standalone.SPLASH) {
        SplashRoute(navController = navController)
    }
}


/**
 * Pantalla/flujo de **Splash (bootstrap)**.
 *
 * Propósito:
 * - Observar el estado de usuario proporcionado por [AuthViewModel] y **redirigir** a
 *   [Graph.AUTH] o [Graph.HOME] según corresponda.
 *
 * Reglas de decisión:
 * - `u == null` → no hay sesión → navegar a **AUTH**.
 * - `u.isAnonymous || u.isEmailVerified` → sesión válida (invitado o verificada) → **HOME**.
 * - En otro caso → forzar `signOut()` y volver a **AUTH**.
 *
 * Detalles de implementación:
 * - Se usa `collectAsStateWithLifecycle()` para observar el flujo `user` respetando el ciclo
 *   de vida de la composición (evita fugas/observaciones en segundo plano).
 * - `LaunchedEffect(userState.value)` relanza su bloque **solo** cuando cambia el valor
 *   observado. Es un patrón seguro para disparar navegación *side-effect* desde estado.
 * - La navegación limpia Splash del back stack con `popUpTo(SPLASH) { inclusive = true }`,
 *   de modo que el usuario no puede volver a Splash con el botón atrás.
 *
 * Notas/precauciones:
 * - Si el flujo `user` emite transiciones rápidas (p. ej., `null → anon → verified`),
 *   este efecto podría navegar varias veces en secuencia. `launchSingleTop = true` ayuda
 *   a evitar duplicados, pero si en el futuro ves “rebotes”, considera **de-bounce** o
 *   mapear a un "destino objetivo" (AUTH/HOME) y navegar sólo si cambia.
 * - `signOut()` dentro del efecto provocará nuevas emisiones del flujo; está bien, ya
 *   que inmediatamente se redirige y se despeja el back stack.
 *
 * UI:
 * - Muestra un `CircularProgressIndicator` centrado mientras se decide el destino.
 *
 * @param navController Controlador de navegación para emitir los cambios de destino.
 * @param viewModel ViewModel inyectado con Hilt scopeado al destino de Splash.
 */
@Composable
private fun SplashRoute(
    navController: NavHostController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val userState = viewModel.user.collectAsStateWithLifecycle()
    val u = userState.value

    // Derivamos el destino objetivo a partir del estado de usuario.
    val target: String = when {
        u == null -> Graph.AUTH
        u.isAnonymous || u.isEmailVerified -> Graph.HOME
        else -> Graph.AUTH // usuario no verificado → irá a AUTH tras signOut()
    }

    // Si el usuario está no verificado, forzaremos logout antes de navegar.
    val shouldSignOut = u != null && !u.isAnonymous && !u.isEmailVerified

    // Navegación **claveada por destino**, no por cada emisión de usuario.
    LaunchedEffect(target) {
        if (shouldSignOut) {
            // Mantiene la semántica actual: salir si no verificado.
            viewModel.signOut()
        }
        // Evita navegación redundante al mismo destino.
        if (navController.currentDestination?.route != target) {
            navController.navigate(target) {
                popUpTo(Standalone.SPLASH) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
