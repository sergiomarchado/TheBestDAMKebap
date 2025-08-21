package com.sergiom.thebestdamkebap.view.home.components

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Diálogo de login reutilizable. Se muestra sólo si [show] es true.
 * Mantiene su propio estado de formulario y delega acciones a callbacks.
 */
@Composable
fun LoginDialogIfNeeded(
    show: Boolean,
    loading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (email: String, password: String) -> Unit,
    onForgot: (email: String) -> Unit,
    onGoRegister: () -> Unit
) {
    if (!show) return

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPass by rememberSaveable { mutableStateOf(false) }

    val emailError = email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val passError  = password.isNotBlank() && password.length < 6
    val formInvalid = email.isBlank() || password.isBlank() || emailError || passError

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text("Iniciar sesión") },
        text = {
            Column {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    isError = emailError
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    singleLine = true,
                    isError = passError,
                    visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { showPass = !showPass }) {
                            Text(if (showPass) "Ocultar" else "Ver")
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { onForgot(email) },
                    enabled = email.isNotBlank()
                ) { Text("¿Olvidaste la contraseña?") }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(email, password) },
                enabled = !formInvalid && !loading
            ) {
                if (loading) CircularProgressIndicator(strokeWidth = 2.dp) else Text("Iniciar sesión")
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = { if (!loading) onDismiss() }
                ) { Text("Cancelar") }
                Spacer(Modifier.width(4.dp))
                FilledTonalButton(
                    onClick = {
                        onDismiss()
                        onGoRegister()
                    }
                ) { Text("Crear cuenta") }
            }
        }
    )
}
