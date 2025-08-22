// view/home/screens/address/AddressEditScreen.kt
package com.sergiom.thebestdamkebap.view.home.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergiom.thebestdamkebap.viewmodel.address.AddressEditViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressEditScreen(
    aid: String?,
    onClose: () -> Unit,
    vm: AddressEditViewModel = hiltViewModel()
) {
    LaunchedEffect(aid) { vm.bootstrap(aid) }

    val ui by vm.ui.collectAsStateWithLifecycle()
    val f = ui.form
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(ui.error) { ui.error?.let { snackbar.showSnackbar(it) } }
    LaunchedEffect(ui.saved) { if (ui.saved) onClose() }

    if (ui.isGuest) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Inicia sesión para continuar")
        }
        return
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Encabezado de página + acción Guardar (como en Profile con los botones arriba)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (aid == null) "Nueva dirección" else "Editar dirección",
                    style = MaterialTheme.typography.headlineSmall
                )
                FilledTonalButton(
                    onClick = { vm.save { onClose() } },
                    enabled = f.canSave && !ui.loading,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary

                    )

                ) { Text("Guardar") }
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
                    label = { Text("Etiqueta (Casa, Trabajo)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )
                OutlinedTextField(
                    value = f.recipientName,
                    onValueChange = { vm.edit { copy(recipientName = it) } },
                    label = { Text("Persona de contacto") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )
                OutlinedTextField(
                    value = f.phone,
                    onValueChange = { vm.edit { copy(phone = it) } },
                    label = { Text("Teléfono") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )

                OutlinedTextField(
                    value = f.street,
                    onValueChange = { vm.edit { copy(street = it) } },
                    isError = f.eStreet != null,
                    supportingText = { f.eStreet?.let { Text(it) } },
                    label = { Text("Calle / Avenida") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = f.number,
                        onValueChange = { vm.edit { copy(number = it) } },
                        isError = f.eNumber != null,
                        supportingText = { f.eNumber?.let { Text(it) } },
                        label = { Text("Número") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large
                    )
                    OutlinedTextField(
                        value = f.floorDoor,
                        onValueChange = { vm.edit { copy(floorDoor = it) } },
                        label = { Text("Piso/puerta") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = f.postalCode,
                        onValueChange = { vm.edit { copy(postalCode = it) } },
                        isError = f.ePostalCode != null,
                        supportingText = { f.ePostalCode?.let { Text(it) } },
                        label = { Text("CP") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large
                    )
                    OutlinedTextField(
                        value = f.city,
                        onValueChange = { vm.edit { copy(city = it) } },
                        isError = f.eCity != null,
                        supportingText = { f.eCity?.let { Text(it) } },
                        label = { Text("Ciudad") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large
                    )
                }
                OutlinedTextField(
                    value = f.province,
                    onValueChange = { vm.edit { copy(province = it) } },
                    label = { Text("Provincia") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )
                OutlinedTextField(
                    value = f.notes,
                    onValueChange = { vm.edit { copy(notes = it) } },
                    label = { Text("Indicaciones") },
                    supportingText = { Text("Opcional") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = f.setAsDefault,
                        onCheckedChange = { vm.toggleDefault(it) }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Establecer como predeterminada")
                }

                if (ui.loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
