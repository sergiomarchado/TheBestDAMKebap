package com.sergiom.thebestdamkebap.view.auth.components.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sergiom.thebestdamkebap.R

@Composable
internal fun AuthButtonsRow(
    loading: Boolean,
    enabledLogin: Boolean,
    onLogin: () -> Unit,
    onRegister: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(
            onClick = onLogin,
            enabled = enabledLogin && !loading,
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
        ) {
            if (loading) CircularProgressIndicator(strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            else Text(
                text = stringResource(R.string.authbutton_login),
                fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = onRegister,
            enabled = !loading,
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
        ) {
            Text(
                text = stringResource(R.string.authbutton_register),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

