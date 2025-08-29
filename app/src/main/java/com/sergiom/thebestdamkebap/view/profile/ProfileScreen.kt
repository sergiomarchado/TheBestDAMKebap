// view/home/products/ProfileScreen.kt
package com.sergiom.thebestdamkebap.view.profile

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergiom.thebestdamkebap.R
import com.sergiom.thebestdamkebap.viewmodel.profile.ProfileViewModel
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

// Extras de estilo/layout
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.surfaceColorAtElevation
import com.sergiom.thebestdamkebap.view.profile.utils.asText

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    // Eventos efímeros → snackbars
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { ev ->
            when (ev) {
                is ProfileViewModel.Event.Info  -> snackbar.showSnackbar(ev.text)
                is ProfileViewModel.Event.Error -> snackbar.showSnackbar(ev.text)
            }
        }
    }

    // Invitado: placeholder
    if (ui.isGuest) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.profile_sign_in_to_edit))
        }
        return
    }

    val loading = ui.loading
    val f = ui.form
    val emailReadOnly = ui.email

    // DatePicker (estado local)
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

    // Colores reutilizables para OutlinedTextField
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        cursorColor = MaterialTheme.colorScheme.primary
    )

    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbar) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.profile_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
            )

            val cardShape = MaterialTheme.shapes.extraLarge
            ElevatedCard(
                shape = cardShape,
                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, cardShape),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    // Email (solo lectura)
                    OutlinedTextField(
                        value = emailReadOnly,
                        onValueChange = {},
                        label = { Text(stringResource(R.string.profile_email_label)) },
                        singleLine = true,
                        readOnly = true,
                        enabled = true,
                        supportingText = { Text(stringResource(R.string.profile_email_hint)) },
                        leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = fieldColors
                    )

                    // Nombre
                    OutlinedTextField(
                        value = f.givenName,
                        onValueChange = viewModel::onGivenNameChange,
                        label = { Text(stringResource(R.string.profile_name_label)) },
                        singleLine = true,
                        isError = f.eGivenName != null,
                        supportingText = { f.eGivenName?.let { Text(it.asText()) } },
                        enabled = !loading,
                        leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = fieldColors
                    )

                    // Apellidos
                    OutlinedTextField(
                        value = f.familyName,
                        onValueChange = viewModel::onFamilyNameChange,
                        label = { Text(stringResource(R.string.profile_family_label)) },
                        singleLine = true,
                        isError = f.eFamilyName != null,
                        supportingText = { f.eFamilyName?.let { Text(it.asText()) } },
                        enabled = !loading,
                        leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = fieldColors
                    )

                    // Teléfono (opcional)
                    OutlinedTextField(
                        value = f.phone,
                        onValueChange = viewModel::onPhoneChange,
                        label = { Text(stringResource(R.string.profile_phone_label)) },
                        singleLine = true,
                        isError = f.ePhone != null,
                        supportingText = {
                            if (f.ePhone != null) Text(f.ePhone.asText())
                            else Text(stringResource(R.string.common_optional))
                        },
                        enabled = !loading,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = fieldColors
                    )

                    // Fecha de nacimiento (opcional)
                    OutlinedTextField(
                        value = birthStr,
                        onValueChange = {},
                        label = { Text(stringResource(R.string.profile_birth_label)) },
                        singleLine = true,
                        readOnly = true,
                        isError = f.eBirthDate != null,
                        supportingText = {
                            if (f.eBirthDate != null) Text(f.eBirthDate.asText())
                            else Text(stringResource(R.string.common_optional))
                        },
                        trailingIcon = {
                            IconButton(onClick = { showDateDialog = true }) {
                                Icon(
                                    Icons.Outlined.CalendarMonth,
                                    contentDescription = stringResource(R.string.profile_birth_pick)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = fieldColors
                    )

                    // Botonera
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = viewModel::onSaveClicked,
                            enabled = !loading && f.canSave,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = MaterialTheme.shapes.large,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                if (loading) stringResource(R.string.profile_saving)
                                else stringResource(R.string.profile_save),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = { viewModel.sendPasswordReset() },
                            enabled = !loading && emailReadOnly.isNotBlank(),
                            shape = MaterialTheme.shapes.large,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                stringResource(R.string.profile_reset_password),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (ui.saved) {
                            AssistChip(
                                onClick = {},
                                enabled = false,
                                label = { Text(stringResource(R.string.profile_saved)) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    leadingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo de selección de fecha
    if (showDateDialog) {
        DatePickerDialog(
            onDismissRequest = { showDateDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onBirthDateChange(dateState.selectedDateMillis)
                    showDateDialog = false
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDateDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        ) {
            DatePicker(state = dateState, showModeToggle = false)
        }
    }
}

/* Helpers UI: devuelve el timestamp de "hoy - years" (a medianoche) */
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



