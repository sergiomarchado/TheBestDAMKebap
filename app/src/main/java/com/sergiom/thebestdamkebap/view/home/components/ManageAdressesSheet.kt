package com.sergiom.thebestdamkebap.view.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Sheet modal para **gestionar direcciones** de envío.
 * Muestra la lista y ofrece acciones para añadir/editar/eliminar.
 *
 * Futuro:
 * - Validaciones y formularios (pueden ser pantallas aparte o un segundo sheet).
 * - Dirección por defecto, alias ("Casa", "Trabajo"), etc.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAddressesSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    addresses: List<AddressUi>,
    onAddAddress: () -> Unit,
    onEditAddress: (AddressUi) -> Unit,
    onDeleteAddress: (AddressUi) -> Unit,
) {
    if (!show) return

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Mis direcciones", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            if (addresses.isEmpty()) {
                Text(
                    "Aún no tienes direcciones guardadas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(addresses, key = { it.id }) { addr ->
                        ElevatedCard {
                            Column(Modifier.padding(12.dp)) {
                                Text(addr.alias ?: "Dirección", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(4.dp))
                                Text(addr.fullLine(), style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { onEditAddress(addr) }) { Text("Editar") }
                                    TextButton(onClick = { onDeleteAddress(addr) }) { Text("Eliminar") }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = onAddAddress) {
                Text("Añadir dirección")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Modelo UI mínimo para direcciones (stub). Lleva sólo lo que la UI necesita. */
data class AddressUi(
    val id: String,
    val alias: String?,        // "Casa", "Trabajo"…
    val street: String,
    val postalCode: String,
    val city: String,
    val province: String?,
    val country: String = "España",
) {
    fun fullLine(): String = buildString {
        append(street)
        append(", ")
        append(postalCode)
        append(" ")
        append(city)
        province?.let { append(", $it") }
        append(", ")
        append(country)
    }
}
