package com.sergiom.thebestdamkebap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sergiom.thebestdamkebap.navigation.AppNavHost
import com.sergiom.thebestdamkebap.ui.theme.TheBestDAMKebapTheme
import dagger.hilt.android.AndroidEntryPoint
/**
 * MainActivity
 *
 * Propósito: actividad raíz que hospeda el árbol de Jetpack Compose y delega la navegación
 * en [AppNavHost]. Aquí sólo se realiza la configuración mínima de ventana/tema y el arranque
 * del contenido Compose.
 *
 * Inyección de dependencias: anotada con [AndroidEntryPoint] para habilitar Hilt en la Activity.
 * Esto permite que, dentro de Compose, se puedan obtener ViewModels con `hiltViewModel()`.
 *
 * Edge-to-edge: se activa para dibujar bajo las barras del sistema; la UI deberá gestionar
 * los insets correctamente para evitar que el contenido quede oculto.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Habilitamos edge-to-edge para permitir que la UI ocupe toda la pantalla.
        enableEdgeToEdge()

        setContent {
            // Habilitamos el Theme de Material3
            TheBestDAMKebapTheme {
                // AppNavHost define el NavController y el grafo de navegación.
                AppNavHost()
            }
        }
    }
}
