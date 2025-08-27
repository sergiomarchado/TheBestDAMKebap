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

/** Añade el destino **Splash** al grafo raíz y delega la lógica de arranque en [SplashRoute]. */
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
 * - `user == null`           → AUTH (no hay sesión).
 * - `isAnonymous == true`    → HOME (invitado permitido).
 * - `isEmailVerified == true`→ HOME (cuenta verificada).
 * - resto (no verificado)    → `signOut()` y luego AUTH.
 */
@Composable
private fun SplashRoute(
    navController: NavHostController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val userState = viewModel.user.collectAsStateWithLifecycle()
    val u = userState.value

    // Anónimos → HOME (cambio principal de esta fase)
    val target: String = when {
        u == null -> Graph.AUTH
        u.isAnonymous -> Graph.HOME
        u.isEmailVerified -> Graph.HOME
        else -> Graph.AUTH
    }

    // Logout forzado solo si es usuario "real" sin verificar
    val shouldSignOut = u != null && !u.isAnonymous && !u.isEmailVerified

    LaunchedEffect(target, shouldSignOut) {
        if (shouldSignOut) {
            viewModel.signOut()
        }
        val current = navController.currentDestination?.route
        if (current != target) {
            navController.navigate(target) {
                popUpTo(Standalone.SPLASH) { inclusive = true }
                launchSingleTop = true
                // restoreState = true // opcional
            }
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
