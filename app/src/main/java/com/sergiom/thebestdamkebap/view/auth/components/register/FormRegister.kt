package com.sergiom.thebestdamkebap.view.auth.components.register

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
/**
 * Formulario de registro (UI).
 *
 * Contiene los campos básicos para crear una cuenta:
 * - Nombre (opcional).
 * - Email.
 * - Contraseña.
 * - Confirmación de contraseña.
 *
 * Incluye validación visual mínima mediante flags de error, y un
 * botón principal de envío.
 *
 * Este composable no gestiona estado propio: recibe valores y callbacks
 * desde fuera, de modo que:
 * - La pantalla superior controla los textos y errores.
 * - El ViewModel maneja la lógica de registro y validación.
 *
 * Diseño:
 * - Separadores verticales para mantener ritmo visual.
 * - `widthIn(max = formMaxWidth)` para que los campos no se estiren demasiado
 *   en pantallas grandes.
 * - El botón final muestra estado de carga (`loading`) y respeta `enabled`.
 *
 * @param name Valor del campo "nombre".
 * @param onName Callback cuando cambia el campo "nombre".
 * @param email Valor del campo "email".
 * @param onEmail Callback cuando cambia el campo "email".
 * @param emailError Si `true`, marca el email como inválido.
 * @param password Valor de la contraseña.
 * @param onPassword Callback cuando cambia la contraseña.
 * @param passVisible Controla si la contraseña se muestra en claro.
 * @param onTogglePassVisible Alterna visibilidad de la contraseña.
 * @param passError Si `true`, marca la contraseña como inválida.
 * @param confirm Valor de la confirmación de contraseña.
 * @param onConfirm Callback cuando cambia la confirmación.
 * @param confirmVisible Controla si la confirmación se muestra en claro.
 * @param onToggleConfirmVisible Alterna visibilidad de la confirmación.
 * @param confirmError Si `true`, marca la confirmación como inválida.
 * @param loading Estado de carga (true = muestra spinner en el botón).
 * @param enabled Si el formulario puede enviarse.
 * @param onSubmit Acción a ejecutar al enviar el formulario.
 * @param formMaxWidth Ancho máximo de los campos (para pantallas grandes).
 */
@Composable
internal fun FormRegister(
    name: String, onName: (String) -> Unit,
    email: String, onEmail: (String) -> Unit, emailError: Boolean,
    password: String, onPassword: (String) -> Unit,
    passVisible: Boolean, onTogglePassVisible: () -> Unit, passError: Boolean,
    confirm: String, onConfirm: (String) -> Unit,
    confirmVisible: Boolean, onToggleConfirmVisible: () -> Unit, confirmError: Boolean,
    loading: Boolean, enabled: Boolean,
    onSubmit: () -> Unit,
    formMaxWidth: Dp
) {
    NameFieldRegister(
        value = name,
        onValueChange = onName,
        modifier = Modifier.widthIn(max = formMaxWidth)
    )

    Spacer(Modifier.height(12.dp))

    EmailFieldRegister(
        value = email,
        onValueChange = onEmail,
        isError = emailError,
        modifier = Modifier.widthIn(max = formMaxWidth)
    )

    Spacer(Modifier.height(12.dp))

    PasswordFieldRegister(
        value = password,
        onValueChange = onPassword,
        visible = passVisible,
        onToggleVisible = onTogglePassVisible,
        isError = passError,
        imeAction = ImeAction.Next,
        onImeAction = null, // no-op en Next
        modifier = Modifier.widthIn(max = formMaxWidth)
    )

    Spacer(Modifier.height(12.dp))

    ConfirmPasswordFieldRegister(
        value = confirm,
        onValueChange = onConfirm,
        visible = confirmVisible,
        onToggleVisible = onToggleConfirmVisible,
        isError = confirmError,
        imeAction = if (enabled) ImeAction.Done else ImeAction.None,
        onImeAction = if (enabled) onSubmit else null,
        modifier = Modifier.widthIn(max = formMaxWidth)
    )

    Spacer(Modifier.height(20.dp))

    RegisterPrimaryButton(
        loading = loading,
        enabled = enabled,
        onClick = onSubmit,
        formMaxWidth = formMaxWidth
    )
}

