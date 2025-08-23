package com.sergiom.thebestdamkebap.view.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Hoja modal para **gestionar direcciones** de envío.
 *
 * Qué muestra:
 * - Un listado compacto de direcciones con acciones **Editar** / **Eliminar**.
 * - Un CTA para **Añadir dirección**.
 *
 * Contrato:
 * - [show]: controla la visibilidad (si es `false`, no compone nada).
 * - [addresses]: lista de direcciones a renderizar (solo datos necesarios para UI).
 * - [onAddAddress], [onEditAddress], [onDeleteAddress]: callbacks a orquestar fuera.
 * - [onDismiss]: se invoca al cerrar el modal (tap fuera/gesto o desde acciones).
 *
 * Escalado futuro (sin romper API):
 * - Dirección por defecto (toggle).
 * - Confirmación al borrar, y/o UNDO con Snackbar.
 * - Filtros/orden (recientes, por alias) si crece el volumen.
 *
 * i18n:
 * - Los textos visibles deberían venir de `strings.xml`.
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

    // Conservar scroll del listado si se recompose el contenido
    val listState = rememberLazyListState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Título marcado como encabezado para lectores de pantalla
            Text(
                "Mis direcciones", // TODO i18n
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(12.dp))

            if (addresses.isEmpty()) {
                Text(
                    "Aún no tienes direcciones guardadas.", // TODO i18n
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    state = listState
                ) {
                    items(addresses, key = { it.id }) { addr ->
                        ElevatedCard(shape = MaterialTheme.shapes.large) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    addr.alias ?: "Dirección", // TODO i18n
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    addr.fullLine(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { onEditAddress(addr) }
                                    ) { Text("Editar") } // TODO i18n

                                    TextButton(
                                        onClick = { onDeleteAddress(addr) }
                                    ) { Text("Eliminar") } // TODO i18n
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            FilledTonalButton(
                onClick = onAddAddress,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Text("Añadir dirección") // TODO i18n
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Modelo UI mínimo para una dirección.
 * Mantiene solo lo imprescindible para renderizar la fila y lanzar acciones.
 */
data class AddressUi(
    val id: String,
    val alias: String?,        // "Casa", "Trabajo"…
    val street: String,
    val postalCode: String,
    val city: String,
    val province: String?,
    val country: String = "España",
) {
    /** Línea única legible para mostrar bajo el título de la tarjeta. */
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
