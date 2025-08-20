package com.sergiom.thebestdamkebap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sergiom.thebestdamkebap.ui.auth.AuthScreen
import com.sergiom.thebestdamkebap.ui.theme.TheBestDAMKebapTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity raíz con Navigation-Compose.
 *
 * Rutas:
 * - "auth" -> AuthScreen (lanza sign-in anónimo y, si hay user, navega a "home").
 * - "home" -> HomePlaceholder (solo por hoy; mañana lo cambiamos por HomeScreen real).
 *
 * Nota: usamos popUpTo(inclusive = true) para que al llegar a 'home' no puedas volver
 * atrás a 'auth' con el botón de back.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private object Routes {
        const val AUTH = "auth"
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
                    startDestination = Routes.AUTH
                ) {
                    composable(Routes.AUTH) {
                        AuthScreen(
                            onAuthenticated = {
                                navController.navigate(Routes.HOME) {
                                    // Evita volver a 'auth' con back
                                    popUpTo(Routes.AUTH) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(Routes.HOME) {
                        // TODO (próxima clase): sustituir por com.sergiom.thebestdamkebap.ui.home.HomeScreen()
                        HomePlaceholder()
                    }
                }
            }
        }
    }
}

/** Placeholder mínimo para 'home' (sin crear nuevos archivos en esta clase). */
@Composable
private fun HomePlaceholder() {
    Scaffold { _ ->
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "The Best DAM Kebap",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                "¡Bienvenido/a! 🎉",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
