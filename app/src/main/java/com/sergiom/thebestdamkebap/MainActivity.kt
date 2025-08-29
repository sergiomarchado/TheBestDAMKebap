package com.sergiom.thebestdamkebap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.sergiom.thebestdamkebap.navigation.AppNavHost
import com.sergiom.thebestdamkebap.ui.theme.TheBestDAMKebapTheme
import dagger.hilt.android.AndroidEntryPoint
/**
 * # MainActivity
 *
 * Actividad raíz de la app. Su única responsabilidad es:
 * - Configurar la ventana en **modo edge-to-edge** (la UI puede dibujar bajo barras del sistema).
 * - Aplicar el **tema Material 3** de la app.
 * - Montar el árbol de **Jetpack Compose** delegando la navegación en [AppNavHost].
 *
 * ## Hilt
 * Anotada con [AndroidEntryPoint] para que los `ViewModel` (y otras dependencias) puedan
 * inyectarse dentro de Composables vía `hiltViewModel()`.
 *
 * ## Insets
 * Al habilitar edge-to-edge, la UI debe manejar insets (status/navigation bars) desde Compose
 * —p. ej., con `Scaffold` + `contentWindowInsets = WindowInsets.safeDrawing` o similar— para que
 * el contenido no quede detrás de las barras del sistema.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Habilitamos edge-to-edge para permitir que la UI ocupe toda la pantalla.
        enableEdgeToEdge()

        setContent {
            // Habilitamos el Theme de Material3
            TheBestDAMKebapTheme {
                // Punto de entrada de navegación: crea/gestiona NavController y el grafo.
                AppNavHost()
            }
        }
    }
}
