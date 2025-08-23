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
// Nota: El AuthViewModel ahora expone DomainUser?, pero desde aquí no necesitas importarlo.

// ─────────────────────────────────────────────────────────────────────────────
// Registro de la ruta Splash en el grafo raíz
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Añade el destino **Splash** al grafo raíz y delega la lógica de arranque en [SplashRoute].
 */
fun NavGraphBuilder.splashRoute(
    navController: NavHostController
) {
    composable(Standalone.SPLASH) {
        SplashRoute(navController = navController)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Pantalla de Splash / Bootstrap
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Pantalla **Splash**: decide a qué grafo ir (AUTH o HOME) según el estado de usuario.
 *
 * Reglas:
 * - `user == null`           → ir a AUTH (no hay sesión).
 * - `isAnonymous == true`    → ir a HOME (invitado permitido).
 * - `isEmailVerified == true`→ ir a HOME (cuenta verificada).
 * - resto (no verificado)    → hacer `signOut()` y luego ir a AUTH.
 *
 * Detalles:
 * - Se observa `viewModel.user` con `collectAsStateWithLifecycle()` para respetar Lifecycle.
 * - La navegación se hace en `LaunchedEffect` (side-effect) y limpiamos Splash del back stack.
 */
@Composable
private fun SplashRoute(
    navController: NavHostController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    // 1) Obtenemos el usuario actual desde el VM (DomainUser?); la UI se recompondrá si cambia.
    val userState = viewModel.user.collectAsStateWithLifecycle()
    val u = userState.value

    // 2) Calculamos el destino objetivo a partir del usuario actual.
    val target: String = when {
        u == null -> Graph.AUTH
        u.isAnonymous || u.isEmailVerified -> Graph.HOME
        else -> Graph.AUTH // usuario logueado pero NO verificado → le sacamos a AUTH
    }

    // 3) ¿Debemos forzar logout? (caso: usuario real pero sin verificar)
    val shouldSignOut = u != null && !u.isAnonymous && !u.isEmailVerified

    // 4) Disparamos navegación como efecto. Importante: claveamos por (target, shouldSignOut)
    //    para cubrir secuencias como null→noVerificado (mismo target=AUTH pero cambia shouldSignOut).
    LaunchedEffect(target, shouldSignOut) {
        if (shouldSignOut) {
            // Cierra sesión para bloquear acceso de cuentas no verificadas.
            viewModel.signOut()
        }
        // Evitamos navegar redundante al mismo destino; `currentDestination` puede ser null al inicio,
        // por eso también usamos launchSingleTop al navegar.
        val current = navController.currentDestination?.route
        if (current != target) {
            navController.navigate(target) {
                // Limpiamos Splash para que no se pueda volver con “atrás”
                popUpTo(Standalone.SPLASH) { inclusive = true }
                // Previene duplicados si ya estamos navegando al mismo sitio
                launchSingleTop = true
                // (Opcional) Si quieres restaurar estado cuando vuelvas a grafos con tabs/listas:
                // restoreState = true
            }
        }
    }

    // 5) UI simple de espera mientras decidimos el destino.
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
