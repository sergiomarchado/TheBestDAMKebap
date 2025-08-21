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

fun NavGraphBuilder.splashRoute(
    navController: NavHostController
) {
    composable(Standalone.SPLASH) {
        SplashRoute(navController = navController)
    }
}

@Composable
private fun SplashRoute(
    navController: NavHostController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val userState = viewModel.user.collectAsStateWithLifecycle()

    LaunchedEffect(userState.value) {
        val u = userState.value
        when {
            // Sin sesión => a AUTH
            u == null -> navController.navigate(Graph.AUTH) {
                popUpTo(Standalone.SPLASH) { inclusive = true }
                launchSingleTop = true
            }
            // Invitado o usuario verificado => HOME
            u.isAnonymous || u.isEmailVerified -> navController.navigate(Graph.HOME) {
                popUpTo(Standalone.SPLASH) { inclusive = true }
                launchSingleTop = true
            }
            // Usuario no verificado => forzamos logout y volvemos a AUTH
            else -> {
                viewModel.signOut()
                navController.navigate(Graph.AUTH) {
                    popUpTo(Standalone.SPLASH) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
