package com.sergiom.thebestdamkebap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sergiom.thebestdamkebap.ui.auth.LoginScreen
import com.sergiom.thebestdamkebap.ui.auth.RegisterScreen
import com.sergiom.thebestdamkebap.ui.theme.TheBestDAMKebapTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity raíz con Navigation-Compose.
 *
 * Rutas:
 * - "login"    -> LoginScreen (email/contraseña + invitado).
 * - "register" -> RegisterScreen (alta o mejora de invitado).
 * - "home"     -> Placeholder de home (lo sustituiremos por HomeScreen real).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private object Routes {
        const val LOGIN = "login"
        const val REGISTER = "register"
        const val HOME = "home"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TheBestDAMKebapTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = Routes.LOGIN
                ) {
                    composable(Routes.LOGIN) {
                        LoginScreen(
                            logoRes = R.drawable.ic_logo,
                            onAuthenticated = {
                                navController.navigate(Routes.HOME) {
                                    // Limpia login del back stack
                                    popUpTo(Routes.LOGIN) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onGoToRegister = {
                                navController.navigate(Routes.REGISTER)
                            }
                        )
                    }

                    composable(Routes.REGISTER) {
                        RegisterScreen(
                            logoRes = R.drawable.ic_logo,
                            onRegistered = {
                                navController.navigate(Routes.HOME) {
                                    // Al registrar, limpia también login del back stack
                                    popUpTo(Routes.LOGIN) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onBackToLogin = { navController.popBackStack() }
                        )
                    }

                    composable(Routes.HOME) {
                        // TODO: sustituir por HomeScreen real en la siguiente clase
                        HomePlaceholder()
                    }
                }
            }
        }
    }
}

/** Placeholder mínimo para 'home' (temporal). */
@Composable
private fun HomePlaceholder() {
    Scaffold { _ ->
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("The Best DAM Kebap", style = MaterialTheme.typography.headlineSmall)
            Text("¡Bienvenido/a! 🎉", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
