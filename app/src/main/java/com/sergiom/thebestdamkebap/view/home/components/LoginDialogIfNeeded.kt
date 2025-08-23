package com.sergiom.thebestdamkebap.view.home.components

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Diálogo reutilizable de **inicio de sesión**.
 *
 * Qué hace:
 * - Se muestra sólo cuando [show] es `true`.
 * - Mantiene estado local del formulario (email/contraseña/mostrar contraseña).
 * - Valida mínimamente y delega acciones en callbacks de la pantalla padre.
 *
 * Notas:
 * - Los textos visibles deberían ir a `strings.xml` para i18n.
 * - El botón de “He olvidado…” sólo se habilita si el email es válido.
 *
 * Callbacks:
 * - [onDismiss]     → cerrar el diálogo.
 * - [onConfirm]     → intentar login con (email, password).
 * - [onForgot]      → solicitar reset de contraseña con el email actual.
 * - [onGoRegister]  → navegar al registro (el diálogo se cierra antes).
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

    // Estado local del formulario (sobrevive rotaciones si es posible).
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPass by rememberSaveable { mutableStateOf(false) }

    // Validación sencilla en cliente (no sustituye la del ViewModel/servidor).
    val emailError = email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val passError  = password.isNotBlank() && password.length < 6
    val formInvalid = email.isBlank() || password.isBlank() || emailError || passError

    val focus = LocalFocusManager.current

    AlertDialog(
        // Evita cerrar durante una operación en curso.
        onDismissRequest = { if (!loading) onDismiss() },

        title = { Text("Iniciar sesión") },

        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    isError = emailError,
                    supportingText = {
                        if (emailError) Text("Introduce un email válido")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )

                // Contraseña
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    singleLine = true,
                    isError = passError,
                    supportingText = {
                        if (passError) Text("Mínimo 6 caracteres")
                    },
                    visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        // IconButton accesible para mostrar/ocultar contraseña
                        IconButton(onClick = { showPass = !showPass }) {
                            val desc = if (showPass) "Ocultar contraseña" else "Mostrar contraseña"
                            Icon(
                                imageVector = if (showPass) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = desc
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (!formInvalid && !loading) {
                                focus.clearFocus()
                                onConfirm(email.trim(), password)
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )

                // “He olvidado mi contraseña”
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { onForgot(email.trim()) },
                        // Sólo si hay email válido (evita errores innecesarios).
                        enabled = email.isNotBlank() && !emailError && !loading
                    ) { Text("¿Olvidaste la contraseña?") }
                }
            }
        },

        // Botón principal (Login)
        confirmButton = {
            Button(
                onClick = {
                    focus.clearFocus()
                    onConfirm(email.trim(), password)
                },
                enabled = !formInvalid && !loading
            ) {
                if (loading) {
                    // Indicador compacto para evitar “bailes” de layout
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Iniciar sesión")
                }
            }
        },

        // Acciones secundarias: Cancelar + Crear cuenta
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
                    },
                    enabled = !loading
                ) { Text("Crear cuenta") }
            }
        }
    )
}
