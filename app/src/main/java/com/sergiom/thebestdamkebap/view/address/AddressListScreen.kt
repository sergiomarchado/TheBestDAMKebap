package com.sergiom.thebestdamkebap.view.address

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergiom.thebestdamkebap.viewmodel.address.AddressListViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * Lista de **direcciones del usuario**.
 *
 * Rol:
 * - Muestra direcciones y permite añadir/editar/eliminar y marcar predeterminada.
 * - Escucha eventos efímeros del VM para snackbars.
 *
 * MVVM:
 * - VM expone `ui` (estado) + `events` (one-shot).
 * - UI observa `ui` con `collectAsStateWithLifecycle()` y reacciona a `events` en `LaunchedEffect`.
 */
@Suppress("KotlinConstantConditions")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressListScreen(
    @Suppress("unused") onBack: () -> Unit,                // se mantiene por compatibilidad (no se usa aquí)
    onAddNew: () -> Unit,
    onEdit: (String) -> Unit,
    vm: AddressListViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    // Eventos efímeros → snackbars
    LaunchedEffect(Unit) {
        vm.events.collectLatest { ev ->
            when (ev) {
                is AddressListViewModel.Event.Info  -> snackbar.showSnackbar(ev.text)
                is AddressListViewModel.Event.Error -> snackbar.showSnackbar(ev.text)
            }
        }
    }

    if (ui.isGuest) {
        // Mensaje para invitado (coherente con Profile)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
            // Encabezado + acción “Añadir”
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mis direcciones",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() }
                )
                FilledTonalButton(
                    onClick = onAddNew,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    enabled = !ui.loading
                ) {
                    // Icono + texto: el texto ya “describe” la acción → contentDescription = null
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Añadir")
                }
            }

            Spacer(Modifier.height(12.dp))

            if (ui.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(items = ui.addresses, key = { it.address.id }) { item ->
                        ElevatedCard(shape = MaterialTheme.shapes.extraLarge) {
                            Column(Modifier.padding(16.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        (item.address.label ?: item.address.street)
                                            .ifBlank { "Dirección" },
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (item.isDefault) {
                                        AssistChip(onClick = {}, label = { Text("Predeterminada") })
                                    }
                                }

                                Spacer(Modifier.height(6.dp))

                                // Línea postal simple (sin i18n compleja por ahora)
                                Text("${item.address.street}, ${item.address.number} ${item.address.floorDoor.orEmpty()}".trim())
                                Text("${item.address.postalCode} ${item.address.city} ${item.address.province.orEmpty()}".trim())

                                Spacer(Modifier.height(10.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { onEdit(item.address.id) }, enabled = !ui.loading) {
                                        Icon(Icons.Outlined.Edit, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Editar")
                                    }
                                    OutlinedButton(onClick = { vm.delete(item.address.id) }, enabled = !ui.loading) {
                                        Icon(Icons.Outlined.Delete, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Eliminar")
                                    }
                                    TextButton(
                                        onClick = { vm.setDefault(item.address.id) },
                                        enabled = !ui.loading && !item.isDefault
                                    ) {
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
