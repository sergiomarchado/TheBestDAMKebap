package com.sergiom.thebestdamkebap.view.home.start.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sergiom.thebestdamkebap.R

@Composable
internal fun AddressBlock(
    addresses: List<Pair<String, String>>,
    selectedId: String?,
    enabled: Boolean,
    onSelect: (String) -> Unit,
    onAddNew: () -> Unit,
    onManage: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rotate by animateFloatAsState(if (expanded) 180f else 0f, label = "rotateExpand")

    Surface(
        tonalElevation = 4.dp,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Fila SIEMPRE visible
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null)
                Text(
                    text = when {
                        addresses.isEmpty() -> stringResource(R.string.addresses_empty_short)
                        selectedId == null  -> stringResource(R.string.addresses_choose_one)
                        else -> addresses.firstOrNull { it.first == selectedId }?.second
                            ?: stringResource(R.string.addresses_choose_one)
                    },
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall
                )
                IconButton(onClick = { expanded = !expanded }, enabled = enabled) {
                    Icon(
                        imageVector = Icons.Outlined.ExpandMore,
                        contentDescription = if (expanded)
                            stringResource(R.string.addresses_collapse_cd)
                        else
                            stringResource(R.string.addresses_expand_cd),
                        modifier = Modifier.graphicsLayer { rotationZ = rotate }
                    )
                }
            }

            // Contenido del desplegable: botones + lista (si hay)
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onAddNew, enabled = enabled) { Text(stringResource(R.string.address_add_cta)) }
                        TextButton(onClick = onManage, enabled = enabled) { Text(stringResource(R.string.addresses_manage_cta)) }
                    }

                    if (addresses.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            addresses.forEach { (id, label) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(
                                        selected = id == selectedId,
                                        onClick = { if (enabled) onSelect(id) },
                                        enabled = enabled
                                    )
                                    Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

