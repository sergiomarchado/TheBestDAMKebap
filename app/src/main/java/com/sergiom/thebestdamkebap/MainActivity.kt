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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sergiom.thebestdamkebap.ui.auth.LoginScreen
import com.sergiom.thebestdamkebap.ui.theme.TheBestDAMKebapTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity raíz con Navigation-Compose.
 *
 * Rutas:
 * - "login"    -> LoginScreen (email/contraseña + invitado).
 * - "register" -> Placeholder de registro (lo creamos de verdad en la próxima clase).
 * - "home"     -> Placeholder de home (lo sustituimos por HomeScreen real luego).
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
                            onAuthenticated = {
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.LOGIN) { inclusive = true } // no volver a login
                                    launchSingleTop = true
                                }
                            },
                            onGoToRegister = {
                                navController.navigate(Routes.REGISTER)
                            }
                        )
                    }

                    composable(Routes.REGISTER) {
                        RegisterPlaceholder(
                            onBack = { navController.popBackStack() }
                        )
                        // Próxima clase: sustituir por RegisterScreen real e invocar
                        // viewModel.registerWithEmail(...) desde ahí.
                    }

                    composable(Routes.HOME) {
                        HomePlaceholder()
                        // Próxima clase: sustituir por HomeScreen real.
                    }
                }
            }
        }
    }
}

/** Placeholder mínimo para 'home' (temporal). */
@androidx.compose.runtime.Composable
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

/** Placeholder mínimo para 'register' (temporal). */
@androidx.compose.runtime.Composable
private fun RegisterPlaceholder(onBack: () -> Unit) {
    Scaffold { _ ->
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Registro (pendiente)", style = MaterialTheme.typography.headlineSmall)
            Text("Aquí irá el formulario de alta.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
