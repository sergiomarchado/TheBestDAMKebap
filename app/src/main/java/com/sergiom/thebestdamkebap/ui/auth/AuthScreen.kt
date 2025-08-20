package com.sergiom.thebestdamkebap.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.util.TableInfo
import com.sergiom.thebestdamkebap.auth.AuthViewModel

/**
 * Pantalla mínima de autenticación.
 *
 * Responsabilidades:
 * - Invocar el inicio anónimo al entrar si no hay usuario.
 * - Observar estado de [AuthViewModel] (usuario, loading, error).
 * - Mostrar un progress durante operaciones, un snackbar para errores y
 *   contenido simple cuando hay sesión.
 *
 * Decisiones:
 * - **Una sola unidad de código** (tu regla): no añadimos navegación ni otros archivos.
 * - Se utiliza `collectAsStateWithLifecycle` para respetar el ciclo de vida de la UI.
 * - Los textos están *hardcoded* por simplicidad; luego internacionalizamos si quieres.
 */

@Composable
fun AuthScreen(
    onAuthenticated:() -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {

    // Estado observable de la VM con awareness de lifecycle
    val user by viewModel.user.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    // Host de snackbar para mostrar errores al usuario
    val snackbarHostState = remember { SnackbarHostState() }

    // Al montar la pantalla, si no hay usuario, intentamos login anónimo
    LaunchedEffect(Unit) {
        viewModel.signInAnonymouslyIfNeeded()
    }

    // Cada vez que llegue un error nuevo, lo mostramos en un snackbar
    LaunchedEffect(error) {
        if(error != null){
            snackbarHostState.showSnackbar(error!!)
        }
    }

    // Cuando hay usuario, avisamos a quién llame
    LaunchedEffect(user?.uid) {
        if(user != null) onAuthenticated()
    }

    Scaffold (
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ){ paddingValues ->
        //Contenido centrado vertical/horizontal
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ){

            when {
                loading -> {
                    // Indicador simple de carga
                    CircularProgressIndicator()
                }

                user != null -> {
                    Text(
                        text = "Sesión Iniciada",
                        style = MaterialTheme.typography.headlineSmall
                        )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "UID: ${user!!.uid}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(onClick = { viewModel.signOut()}){
                        Text(text = "Cerrar Sesión")
                    }
                }

                else -> {
                    // Caso raro: sin loading y sin usuario -> ofrecer reintento
                    Text(
                        text = "No hay sesión activa",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(16.dp))

                    Button(onClick = { viewModel.signInAnonymouslyIfNeeded()}) {
                        Text(text = "Iniciar Sesión de manera anónima")
                    }
                }
            }
        }
    }

}

