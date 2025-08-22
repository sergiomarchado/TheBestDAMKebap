// view/home/screens/address/AddressListScreen.kt
package com.sergiom.thebestdamkebap.view.home.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergiom.thebestdamkebap.viewmodel.address.AddressListViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressListScreen(
    onBack: () -> Unit,                // ← se mantiene por compatibilidad (no se usa)
    onAddNew: () -> Unit,
    onEdit: (String) -> Unit,
    vm: AddressListViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.events.collectLatest { ev ->
            when (ev) {
                is AddressListViewModel.Event.Info  -> snackbar.showSnackbar(ev.text)
                is AddressListViewModel.Event.Error -> snackbar.showSnackbar(ev.text)
            }
        }
    }

    if (ui.isGuest) {
        // Igual que Profile: mensaje simple dentro del shell
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("Inicia sesión para gestionar tus direcciones")
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
            // Encabezado de página + acción "Añadir", como en Profile
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text("Mis direcciones", style = MaterialTheme.typography.headlineSmall)
                FilledTonalButton(
                    onClick = onAddNew,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary

                    )
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Añadir")
                }
            }

            Spacer(Modifier.height(12.dp))

            if (ui.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = ui.addresses, key = { it.address.id }) { item ->
                        ElevatedCard(shape = MaterialTheme.shapes.extraLarge) {
                            Column(Modifier.padding(16.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        (item.address.label ?: item.address.street).ifBlank { "Dirección" },
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (item.isDefault) AssistChip(onClick = {}, label = { Text("Predeterminada") })
                                }
                                Spacer(Modifier.height(6.dp))
                                Text("${item.address.street}, ${item.address.number} ${item.address.floorDoor.orEmpty()}".trim())
                                Text("${item.address.postalCode} ${item.address.city} ${item.address.province.orEmpty()}".trim())

                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { onEdit(item.address.id) }) {
                                        Icon(Icons.Outlined.Edit, null); Spacer(Modifier.width(6.dp)); Text("Editar")
                                    }
                                    OutlinedButton(onClick = { vm.delete(item.address.id) }) {
                                        Icon(Icons.Outlined.Delete, null); Spacer(Modifier.width(6.dp)); Text("Eliminar")
                                    }
                                    TextButton(onClick = { vm.setDefault(item.address.id) }) {
                                        Text(if (item.isDefault) "Es la predeterminada" else "Hacer predeterminada")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
