// view/home/products/address/AddressEditScreen.kt
package com.sergiom.thebestdamkebap.view.address

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergiom.thebestdamkebap.view.address.utils.asText
import com.sergiom.thebestdamkebap.viewmodel.address.AddressEditViewModel
import com.sergiom.thebestdamkebap.R

/**
 * Pantalla de **crear/editar dirección**.
 *
 * Flujo:
 * - `LaunchedEffect(aid)` → `vm.bootstrap(aid)`.
 * - Formulario con validaciones en línea y botón Guardar.
 * - En éxito, el VM ejecuta el callback de `save { onClose() }` y esta pantalla se cierra.
 *
 * UX/A11y:
 * - Campos deshabilitados mientras `ui.loading`.
 * - IME Actions configuradas (Next/Done) para navegación con teclado y confirmación con Done.
 * - `imePadding()` para que el teclado no tape los campos inferiores.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressEditScreen(
    aid: String?,
    onClose: (String) -> Unit,
    vm: AddressEditViewModel = hiltViewModel()
) {
    // Bootstrap controlado por parametro de navegación
    LaunchedEffect(aid) { vm.bootstrap(aid) }

    val ui by vm.ui.collectAsStateWithLifecycle()
    val f = ui.form
    val snackbar = remember { SnackbarHostState() }

    // Feedback efímero (error simple)
    LaunchedEffect(ui.error) { ui.error?.let { snackbar.showSnackbar(it) } }

    if (ui.isGuest) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = stringResource(R.string.login_request))
        }
        return
    }

    // Derivados de UI para legibilidad
    val isEditing = aid != null
    val saveEnabled = f.canSave && !ui.loading

    // Acción de guardado (se usa en botón y en IME Done)
    val onSave = remember(ui.loading, f.canSave) {
        {
            if (saveEnabled) vm.save {id -> onClose(id) }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .imePadding(), // evita que el teclado tape los campos inferiores
        ) {
            // Encabezado + Guardar
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text =
                        if (!isEditing) stringResource(R.string.new_address)
                        else stringResource(R.string.edit_address),
                    style = MaterialTheme.typography.headlineSmall
                )
                FilledTonalButton(
                    onClick = onSave,
                    enabled = saveEnabled,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(text =
                        if (ui.loading) stringResource(R.string.saving)
                        else stringResource(R.string.save))
                }
            }

            Spacer(Modifier.height(12.dp))

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = f.label,
                    onValueChange = { vm.edit { copy(label = it) } },
                    label = { Text(text = stringResource(R.string.address_label) ) },
                    enabled = !ui.loading,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )
                OutlinedTextField(
                    value = f.recipientName,
                    onValueChange = { vm.edit { copy(recipientName = it) } },
                    label = { Text(text = stringResource(R.string.address_recipient_label)) },
                    enabled = !ui.loading,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )
                OutlinedTextField(
                    value = f.phone,
                    onValueChange = { vm.edit { copy(phone = it) } },
                    label = { Text(text = stringResource(R.string.address_phone_label)) },
                    supportingText = {
                        Text(f.ePhone?.asText() ?: stringResource(R.string.address_phone_hint))
                    },
                    isError = f.ePhone != null,
                    enabled = !ui.loading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )

                OutlinedTextField(
                    value = f.street,
                    onValueChange = { vm.edit { copy(street = it) } },
                    label = { Text(text = stringResource(R.string.address_street_label)) },
                    isError = f.eStreet != null,
                    supportingText = { f.eStreet?.let { Text(it.asText()) } },
                    enabled = !ui.loading,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = f.number,
                        onValueChange = { vm.edit { copy(number = it) } },
                        label = { Text(text = stringResource(R.string.address_number_label)) },
                        isError = f.eNumber != null,
                        supportingText = { f.eNumber?.let { Text(it.asText()) } },
                        enabled = !ui.loading,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large
                    )
                    OutlinedTextField(
                        value = f.floorDoor,
                        onValueChange = { vm.edit { copy(floorDoor = it) } },
                        label = { Text(text = stringResource(R.string.address_floor_door_label)) },
                        enabled = !ui.loading,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = f.postalCode,
                        onValueChange = { vm.edit { copy(postalCode = it) } },
                        label = { Text(text = stringResource(R.string.address_postal_code_label)) },
                        isError = f.ePostalCode != null,
                        supportingText = { f.ePostalCode?.let { Text(it.asText()) } },
                        enabled = !ui.loading,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large
                    )
                    OutlinedTextField(
                        value = f.city,
                        onValueChange = { vm.edit { copy(city = it) } },
                        label = { Text(text = stringResource(R.string.address_city_label)) },
                        isError = f.eCity != null,
                        supportingText = { f.eCity?.let { Text(it.asText()) } },
                        enabled = !ui.loading,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large
                    )
                }
                OutlinedTextField(
                    value = f.province,
                    onValueChange = { vm.edit { copy(province = it) } },
                    label = { Text(text = stringResource(R.string.address_province_label)) },
                    enabled = !ui.loading,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )
                OutlinedTextField(
                    value = f.notes,
                    onValueChange = { vm.edit { copy(notes = it) } },
                    label = { Text(text = stringResource(R.string.address_notes_label)) },
                    supportingText = { Text(text = stringResource(R.string.common_optional)) },
                    enabled = !ui.loading,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSave() }),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = f.setAsDefault,
                        onCheckedChange = { vm.toggleDefault(it) },
                        enabled = !ui.loading
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.address_set_as_default))
                }

                if (ui.loading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
