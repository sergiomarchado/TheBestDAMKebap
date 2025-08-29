package com.sergiom.thebestdamkebap.view.address

// Para FlowRow (distribuye los botones en varias líneas si hace falta)
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergiom.thebestdamkebap.R
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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddressListScreen(
    @Suppress("unused") onBack: () -> Unit,                // se mantiene por compatibilidad (no se usa aquí)
    onAddNew: () -> Unit,
    onEdit: (String) -> Unit,
    onSelect: (String) -> Unit,
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
            Text(text = stringResource(R.string.addresses_sign_in_to_manage))
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
                    text = stringResource(R.string.addresses_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() }
                )
                FilledTonalButton(
                    onClick = onAddNew,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    enabled = !ui.loading,
                    shape = MaterialTheme.shapes.large,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.addresses_add),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                        val cardShape = MaterialTheme.shapes.extraLarge
                        ElevatedCard(
                            // Borde en color primario (M3: vía Modifier.border)
                            modifier = Modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary), cardShape),
                            shape = cardShape,
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                            ),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                ) {
                                    Text(
                                        (item.address.label ?: item.address.street)
                                            .ifBlank { stringResource(R.string.addresses_fallback_label) },
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (item.isDefault) {
                                        // Chip “Predeterminada” decorativa con buen contraste en dark
                                        AssistChip(
                                            onClick = {},
                                            enabled = false,
                                            label = {
                                                Text(
                                                    text = stringResource(R.string.address_default_badge),
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                disabledContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                                                disabledLabelColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                                            )
                                        )
                                    }
                                }

                                Spacer(Modifier.height(8.dp))
                                HorizontalDivider(
                                    thickness = 0.75.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                )
                                Spacer(Modifier.height(8.dp))

                                // Dirección
                                Text(
                                    "${item.address.street}, ${item.address.number} ${item.address.floorDoor.orEmpty()}".trim(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${item.address.postalCode} ${item.address.city} ${item.address.province.orEmpty()}".trim(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(Modifier.height(12.dp))

                                // Acciones: FlowRow evita que el texto se parta raro
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onEdit(item.address.id) },
                                        enabled = !ui.loading,
                                        shape = MaterialTheme.shapes.large,
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Icon(Icons.Outlined.Edit, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.addresses_edit),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { vm.delete(item.address.id) },
                                        enabled = !ui.loading,
                                        shape = MaterialTheme.shapes.large,
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Icon(Icons.Outlined.Delete, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.addresses_delete),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    TextButton(
                                        onClick = { vm.setDefault(item.address.id) },
                                        enabled = !ui.loading && !item.isDefault,
                                        shape = MaterialTheme.shapes.large,
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Icon(Icons.Outlined.StarOutline, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text=
                                                if (item.isDefault) stringResource(R.string.addresses_is_default)
                                                else stringResource(R.string.addresses_make_default),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    TextButton(
                                        onClick = { onSelect(item.address.id) },   // devuelve id seleccionado
                                        enabled = !ui.loading,
                                        shape = MaterialTheme.shapes.large,
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.addresses_use_this_address),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
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
