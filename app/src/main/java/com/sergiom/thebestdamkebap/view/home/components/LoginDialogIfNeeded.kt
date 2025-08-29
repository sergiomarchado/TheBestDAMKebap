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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.sergiom.thebestdamkebap.R

/**
 * Diálogo reutilizable de **inicio de sesión**.
 *
 * - Si [isGuest] es `true`, al confirmar muestra un aviso de que se reemplazará
 *   la sesión de invitado antes de llamar a [onConfirm].
 */
@Composable
fun LoginDialogIfNeeded(
    show: Boolean,
    loading: Boolean,
    isGuest: Boolean,                     // ⬅️ NUEVO
    onDismiss: () -> Unit,
    onConfirm: (email: String, password: String) -> Unit,
    onForgot: (email: String) -> Unit,
    onGoRegister: () -> Unit
) {
    if (!show) return

    // Estado local del formulario
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPass by rememberSaveable { mutableStateOf(false) }

    // Confirmación para reemplazar sesión invitado
    var showReplaceAnonDialog by rememberSaveable { mutableStateOf(false) }

    // Validación cliente
    val emailError = email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val passError  = password.isNotBlank() && password.length < 6
    val formInvalid = email.isBlank() || password.isBlank() || emailError || passError

    val focus = LocalFocusManager.current

    // Acción centralizada de login (con confirmación si es invitado)
    fun tryLogin() {
        if (formInvalid || loading) return
        val e = email.trim()
        val p = password
        if (isGuest) {
            showReplaceAnonDialog = true
        } else {
            onConfirm(e, p)
        }
    }

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text(stringResource(R.string.auth_sign_in)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.auth_email_label)) },
                    singleLine = true,
                    isError = emailError,
                    supportingText = { if (emailError) Text(stringResource(R.string.auth_email_invalid)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.auth_password_label)) },
                    singleLine = true,
                    isError = passError,
                    supportingText = { if (passError) Text(stringResource(R.string.auth_password_min_length, 6)) },
                    visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPass = !showPass }) {
                            val desc = if (showPass)
                                stringResource(R.string.auth_hide_password)
                            else
                                stringResource(R.string.auth_show_password)
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
                            focus.clearFocus()
                            tryLogin()
                        }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { onForgot(email.trim()) },
                        enabled = email.isNotBlank() && !emailError && !loading
                    ) { Text(stringResource(R.string.auth_forgot_password_link)) }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    focus.clearFocus()
                    tryLogin()
                },
                enabled = !formInvalid && !loading
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.auth_sign_in))
                }
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { if (!loading) onDismiss() }) {
                    Text(stringResource(R.string.common_cancel))
                }
                Spacer(Modifier.width(4.dp))
                FilledTonalButton(
                    onClick = {
                        onDismiss()
                        onGoRegister()
                    },
                    enabled = !loading
                ) { Text(stringResource(R.string.auth_create_account)) }
            }
        }
    )

    // Diálogo de confirmación si es invitado
    if (showReplaceAnonDialog) {
        AlertDialog(
            onDismissRequest = { showReplaceAnonDialog = false },
            title = { Text(stringResource(R.string.auth_replace_guest_title)) },
            text = {
                Text(stringResource(R.string.auth_replace_guest_message))
            },
            confirmButton = {
                TextButton(onClick = {
                    showReplaceAnonDialog = false
                    onConfirm(email.trim(), password)
                }) { Text(stringResource(R.string.common_continue)) }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceAnonDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }
}
