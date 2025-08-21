package com.sergiom.thebestdamkebap.view.auth.components.register

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation

@Composable
internal fun ConfirmPasswordFieldRegister(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    isError: Boolean,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Repita la contraseña") },
        singleLine = true,
        isError = isError,
        supportingText = { if (isError) Text("Las contraseñas no coinciden") },
        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.Black) },
        trailingIcon = {
            val cd = if (visible) "Ocultar contraseña" else "Mostrar contraseña"
            IconButton(onClick = onToggleVisible) {
                Icon(
                    imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = cd,
                    tint = colors.primary
                )
            }
        },
        visualTransformation = if (visible)
            androidx.compose.ui.text.input.VisualTransformation.None
        else
            PasswordVisualTransformation(),
        shape = MaterialTheme.shapes.medium,
        colors = registerTextFieldColors(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = { onImeAction?.invoke() },
            onDone = { onImeAction?.invoke() }
        ),
        modifier = modifier.fillMaxWidth()
    )
}

