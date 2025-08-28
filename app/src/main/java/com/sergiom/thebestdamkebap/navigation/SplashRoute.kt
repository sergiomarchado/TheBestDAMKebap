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

// ─────────────────────────────────────────────────────────────────────────────
// Registro de la ruta Splash en el grafo raíz
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Añade el destino **Splash** al grafo raíz.
 *
 * @param navController controlador de navegación que permite moverse a otros grafos.
 *
 * Nota:
 * - Delegamos la lógica real en [SplashRoute], así mantenemos separada la
 *   definición de la navegación de la implementación de la pantalla.
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

// ─────────────────────────────────────────────────────────────────────────────
// Pantalla de Splash / Bootstrap
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Pantalla **Splash**: decide a qué grafo ir (AUTH o HOME) según el estado del usuario.
 *
 * Reglas de negocio:
 * - `user == null`            → navegar a AUTH (no hay sesión iniciada).
 * - `u.isAnonymous == true`   → navegar a HOME (usuario invitado permitido).
 * - `u.isEmailVerified == true` → navegar a HOME (cuenta verificada).
 * - resto (usuario no verificado) → `signOut()` y después navegar a AUTH.
 *
 * Comportamiento:
 * - Mientras decide, se muestra un [CircularProgressIndicator].
 * - Se usa [LaunchedEffect] para ejecutar efectos de navegación y logout
 *   **una sola vez** cuando cambian `target` o `shouldSignOut`.
 *
 * @param navController controlador de navegación, usado para redirigir a AUTH o HOME.
 * @param viewModel [AuthViewModel] que expone el estado del usuario (inyectado con Hilt).
 */
@Composable
private fun SplashRoute(
    navController: NavHostController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    // Estado reactivo del usuario (se actualiza con el ciclo de vida de Compose).
    val userState = viewModel.user.collectAsStateWithLifecycle()
    val u = userState.value

    // Lógica de decisión: calcula a qué grafo ir.
    val target: String = when {
        u == null -> Graph.AUTH
        u.isAnonymous -> Graph.HOME
        u.isEmailVerified -> Graph.HOME
        else -> Graph.AUTH
    }

    // Si es usuario real sin verificar, forzamos logout.
    val shouldSignOut = u != null && !u.isAnonymous && !u.isEmailVerified

    // Efecto de navegación (solo se dispara al cambiar target o shouldSignOut).
    LaunchedEffect(target, shouldSignOut) {
        if (shouldSignOut) {
            viewModel.signOut()
        }
        val current = navController.currentDestination?.route
        if (current != target) {
            navController.navigate(target) {

                // Eliminamos Splash del back stack (no volver atrás a esta pantalla).
                popUpTo(Standalone.SPLASH) { inclusive = true }
                launchSingleTop = true  // evita duplicar destinos en el stack

            }
        }
    }

    // UI simple: indicador de carga centrado en pantalla.
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
