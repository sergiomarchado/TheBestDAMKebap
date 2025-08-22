// view/home/screens/ProfileScreen.kt
package com.sergiom.thebestdamkebap.view.home.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergiom.thebestdamkebap.viewmodel.profile.ProfileViewModel
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { ev ->
            when (ev) {
                is ProfileViewModel.Event.Info  -> snackbar.showSnackbar(ev.text)
                is ProfileViewModel.Event.Error -> snackbar.showSnackbar(ev.text)
            }
        }
    }

    if (ui.isGuest) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Inicia sesión para editar tu perfil")
        }
        return
    }

    val loading = ui.loading
    val f = ui.form
    val emailReadOnly = ui.email

    // DatePicker dialog (estado puramente de UI)
    var showDateDialog by rememberSaveable { mutableStateOf(false) }
    val selectablePastDates = remember {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) =
                utcTimeMillis <= System.currentTimeMillis()
        }
    }
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = f.birthDateMillis ?: yearsAgo(18),
        selectableDates = selectablePastDates
    )
    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val birthStr = remember(f.birthDateMillis) {
        f.birthDateMillis?.let { dateFmt.format(Date(it)) }.orEmpty()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbar) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Mi perfil", style = MaterialTheme.typography.headlineSmall)

            ElevatedCard(
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    OutlinedTextField(
                        value = emailReadOnly,
                        onValueChange = {},
                        label = { Text("Email") },
                        singleLine = true,
                        readOnly = true,
                        enabled = true,
                        supportingText = { Text("Este es tu email de acceso. No se puede cambiar desde la app.") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    )

                    OutlinedTextField(
                        value = f.givenName,
                        onValueChange = viewModel::onGivenNameChange,
                        label = { Text("Nombre") },
                        singleLine = true,
                        isError = f.eGivenName != null,
                        supportingText = { f.eGivenName?.let { Text(it) } },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    )

                    OutlinedTextField(
                        value = f.familyName,
                        onValueChange = viewModel::onFamilyNameChange,
                        label = { Text("Apellidos") },
                        singleLine = true,
                        isError = f.eFamilyName != null,
                        supportingText = { f.eFamilyName?.let { Text(it) } },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    )

                    OutlinedTextField(
                        value = f.phone,
                        onValueChange = viewModel::onPhoneChange,
                        label = { Text("Teléfono") },
                        singleLine = true,
                        isError = f.ePhone != null,
                        supportingText = { Text(f.ePhone ?: "Opcional") },
                        enabled = !loading,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    )

                    OutlinedTextField(
                        value = birthStr,
                        onValueChange = {},
                        label = { Text("Fecha de nacimiento") },
                        singleLine = true,
                        readOnly = true,
                        isError = f.eBirthDate != null,
                        supportingText = {
                            if (f.eBirthDate != null) Text(f.eBirthDate)
                            else Text("Opcional. ¡Nos encantará felicitarte! 🥳")
                        },
                        trailingIcon = {
                            IconButton(onClick = { showDateDialog = true }) {
                                Icon(Icons.Outlined.CalendarMonth, contentDescription = "Elegir fecha")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = viewModel::onSaveClicked,
                            enabled = !loading && f.canSave,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(if (loading) "Guardando..." else "Guardar cambios")
                        }

                        OutlinedButton(
                            onClick = viewModel::sendPasswordReset,
                            enabled = !loading && emailReadOnly.isNotBlank()
                        ) { Text("Restablecer contraseña") }

                        if (ui.saved) {
                            AssistChip(onClick = {}, label = { Text("Guardado") })
                        }
                    }
                }
            }
        }
    }

    if (showDateDialog) {
        DatePickerDialog(
            onDismissRequest = { showDateDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onBirthDateChange(dateState.selectedDateMillis)
                    showDateDialog = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDateDialog = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = dateState, showModeToggle = false)
        }
    }
}

/* Helpers UI */
@Suppress("SameParameterValue")
private fun yearsAgo(years: Int): Long {
    val cal = Calendar.getInstance()
    cal.add(Calendar.YEAR, -years)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
