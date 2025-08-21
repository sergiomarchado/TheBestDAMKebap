package com.sergiom.thebestdamkebap.view.auth.components.register

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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

