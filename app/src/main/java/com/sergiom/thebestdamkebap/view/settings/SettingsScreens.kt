package com.sergiom.thebestdamkebap.view.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergiom.thebestdamkebap.R
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
            TopAppBar(title = { Text(stringResource(R.string.settings_title)) }, scrollBehavior = scrollBehavior)
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
            /* ───────── Idioma ───────── */
            Text(
                stringResource(R.string.settings_language_section_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                stringResource(R.string.settings_language_section_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LanguageRow(
                currentTag = ui.languageTag,           // null => Sistema
                enabled = !ui.loading,
                onSelect = { tagOrNull -> vm.setLanguage(tagOrNull) }
            )

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            /* ───────── Eliminar cuenta ───────── */
            Text(
                stringResource(R.string.settings_privacy_section_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(stringResource(R.string.settings_delete_account_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.settings_delete_account_desc),
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
                Spacer(Modifier.width(8.dp))
                Text(if (ui.loading) stringResource(R.string.settings_deleting) else stringResource(R.string.settings_delete_account_cta))
            }

            if (ui.isGuest) {
                Text(
                    stringResource(R.string.settings_sign_in_to_manage),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (ui.error != null && !ui.loading) {
                Text(ui.error!!, color = MaterialTheme.colorScheme.error)
            }

            if (ui.success) {
                Text(stringResource(R.string.settings_deleted_success), color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    /* ───────── Diálogo de confirmación (borrado) ───────── */
    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { if (!ui.loading) showConfirm = false },
            title = { Text(stringResource(R.string.settings_delete_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.settings_delete_dialog_message))
                    if (!ui.isGuest && !ui.email.isNullOrBlank()) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(stringResource(R.string.settings_password_of_email, ui.email!!)) },
                            enabled = !ui.loading,
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (ui.needsReauth) {
                            Text(
                                stringResource(R.string.settings_reauth_hint),
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
                ) { Text(if (ui.loading) stringResource(R.string.settings_deleting) else stringResource(R.string.settings_yes_delete)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { if (!ui.loading) showConfirm = false },
                    enabled = !ui.loading
                ) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }
}

@Composable
private fun LanguageRow(
    currentTag: String?,               // null => Sistema
    enabled: Boolean,
    onSelect: (String?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = currentTag == null,
            onClick = { onSelect(null) },
            enabled = enabled,
            label = { Text(stringResource(R.string.settings_language_system)) }
        )
        FilterChip(
            selected = currentTag == "es",
            onClick = { onSelect("es") },
            enabled = enabled,
            label = { Text(stringResource(R.string.settings_language_spanish)) }
        )
        FilterChip(
            selected = currentTag == "en",
            onClick = { onSelect("en") },
            enabled = enabled,
            label = { Text(stringResource(R.string.settings_language_english)) }
        )
    }
}
