package com.sergiom.thebestdamkebap.view.home.start.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sergiom.thebestdamkebap.viewmodel.home.homestart.HomeStartViewModel

@Composable
internal fun ModeToggle(
    mode: HomeStartViewModel.Mode,
    enabled: Boolean,
    onChange: (HomeStartViewModel.Mode) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = mode == HomeStartViewModel.Mode.DELIVERY,
            onClick = { onChange(HomeStartViewModel.Mode.DELIVERY) },
            label = { Text("A domicilio") },
            enabled = enabled,
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            )
        )
        FilterChip(
            selected = mode == HomeStartViewModel.Mode.PICKUP,
            onClick = { onChange(HomeStartViewModel.Mode.PICKUP) },
            label = { Text("Para recoger") },
            enabled = enabled,
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            )
        )
    }
}
