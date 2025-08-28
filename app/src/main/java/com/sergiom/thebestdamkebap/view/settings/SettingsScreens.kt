package com.sergiom.thebestdamkebap.view.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergiom.thebestdamkebap.viewmodel.settings.SettingsViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: SettingsViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    // Eventos efímeros → snackbars
    LaunchedEffect(Unit) {
        vm.events.collectLatest { ev ->
            when (ev) {
                is SettingsViewModel.Event.Info  -> snackbar.showSnackbar(ev.msg)
                is SettingsViewModel.Event.Error -> snackbar.showSnackbar(ev.msg)
            }
        }
    }

    var showConfirm by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
            TopAppBar(
                title = { Text("Configuración") },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Preferencias (próximamente)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            /* ───────── Eliminar cuenta ───────── */
            Text(
                "Privacidad",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                "Eliminar cuenta",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "Borra tu perfil y direcciones. Tus pedidos existentes no se eliminan por motivos legales/auditoría.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FilledTonalButton(
                onClick = { showConfirm = true },
                enabled = !ui.loading && !ui.isGuest,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Outlined.DeleteForever, contentDescription = null)
                Spacer(Modifier.height(0.dp))
                Text(if (ui.loading) "Eliminando..." else "Eliminar cuenta")
            }

            if (ui.isGuest) {
                Text(
                    "Inicia sesión para gestionar tu cuenta.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (ui.error != null && !ui.loading) {
                Text(ui.error!!, color = MaterialTheme.colorScheme.error)
            }

            if (ui.success) {
                Text("Cuenta eliminada. Se cerró la sesión.", color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    /* ───────── Diálogo de confirmación ───────── */
    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { if (!ui.loading) showConfirm = false },
            title = { Text("¿Eliminar tu cuenta?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Esta acción es permanente. Se borrará tu perfil y tus direcciones.")
                    // Si el usuario tiene email, permite introducir contraseña para re-autenticación rápida
                    if (!ui.isGuest && !ui.email.isNullOrBlank()) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Contraseña de ${ui.email}") },
                            enabled = !ui.loading,
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (ui.needsReauth) {
                            Text(
                                "Por seguridad, introduce la contraseña o vuelve a iniciar sesión.",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteAccount(confirmPassword = if (ui.email.isNullOrBlank()) null else password.ifBlank { null })
                    },
                    enabled = !ui.loading
                ) { Text(if (ui.loading) "Eliminando…" else "Sí, borrar") }
            },
            dismissButton = {
                TextButton(
                    onClick = { if (!ui.loading) showConfirm = false },
                    enabled = !ui.loading
                ) { Text("Cancelar") }
            }
        )
    }
}
