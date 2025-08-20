package com.sergiom.thebestdamkebap.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergiom.thebestdamkebap.auth.AuthViewModel

/**
 * Pantalla de Login (email/contraseña) con:
 * - Acceso como invitado (anónimo).
 * - Enlace “¿Olvidaste la contraseña?”.
 * - Callback [onAuthenticated] para navegar cuando ya haya usuario.
 * - Callback [onGoToRegister] para ir a la pantalla de registro.
 *
 * Decisiones:
 * - Una sola clase, sin icons ni estilos extra (los colores/estética los afinamos luego en Theme).
 * - Sin persistencia de “recordar”, eso lo haremos más adelante (DataStore).
 */

@Composable
fun LoginScreen(
    onAuthenticated: () -> Unit = {},
    onGoToRegister: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    // Estados de VM
    val user by viewModel.user.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    // Estado local del formulario (se guarda al rotar por rememberSaveable)
    val (email, setEmail) = rememberSaveable { mutableStateOf("") }
    val (password, setPassword) = rememberSaveable { mutableStateOf("") }
    val (passwordVisible, setPasswordVisible) =
        rememberSaveable { mutableStateOf(false) }

    // Snackbar para mensajes/errores
    val snackbarHostState = remember { SnackbarHostState() }

    // Si hay usuario, avisamos para navegar (MainActivity hace el navigate)
    LaunchedEffect(user?.uid) {
        if(user!= null) onAuthenticated()
    }

    // Mostrar mensajes desde VM
    LaunchedEffect(error) {
        if(error!=null) snackbarHostState.showSnackbar(error!!)
    }

    LaunchedEffect(message) {
        if(message!=null) snackbarHostState.showSnackbar(message!!)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("¡Bienvenid@!", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text("Inicia sesión o accede como invitado",
                style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(24.dp))

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = setEmail,
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(Modifier.height(12.dp))

            // Password
            OutlinedTextField(
                value = password,
                onValueChange = setPassword,
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = if (passwordVisible) {
                    // Mantenerlo simple sin icons por ahora
                    androidx.compose.ui.text.input.VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    TextButton(onClick = { setPasswordVisible(!passwordVisible) }) {
                        Text(if (passwordVisible) "Ocultar" else "Mostrar")
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                )
            )

            Spacer(Modifier.height(8.dp))

            // ¿Olvidaste la contraseña?
            TextButton(
                onClick = { viewModel.sendPasswordReset(email) },
                enabled = !loading
            ) { Text("¿Has olvidado la contraseña?") }

            Spacer(Modifier.height(8.dp))

            // Iniciar sesión
            Button(
                onClick = { viewModel.signInWithEmail(email.trim(), password) },
                enabled = !loading
            ) {
                if (loading) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                } else {
                    Text("Iniciar sesión")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Acceder como invitado (anónimo)
            TextButton(
                onClick = { viewModel.signInAnonymouslyIfNeeded() },
                enabled = !loading
            ) { Text("Acceder como invitado") }

            Spacer(Modifier.height(12.dp))

            // Ir a registro
            TextButton(
                onClick = onGoToRegister,
                enabled = !loading
            ) { Text("¿No tienes cuenta? Regístrate") }
        }
    }



}
