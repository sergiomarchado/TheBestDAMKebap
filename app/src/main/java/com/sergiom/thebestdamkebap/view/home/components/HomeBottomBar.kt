package com.sergiom.thebestdamkebap.view.home.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

data class HomeNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String,
)

@Composable
internal fun HomeBottomBar(
    items: List<HomeNavItem>,
    currentRoute: String?,
    onItemClick: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    NavigationBar(
        containerColor = colors.primary,       // barra “ámbar” (ajustada a tu tema)
        contentColor = colors.onPrimary
    ) {
        items.forEach { item ->
            val selected = item.route == currentRoute
            NavigationBarItem(
                selected = selected,
                onClick = { onItemClick(item.route) },
                icon = { androidx.compose.material3.Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.onPrimary,
                    selectedTextColor = colors.onPrimary,
                    indicatorColor = colors.primaryContainer,
                    unselectedIconColor = colors.onPrimary.copy(alpha = 0.8f),
                    unselectedTextColor = colors.onPrimary.copy(alpha = 0.8f)
                )
            )
        }
    }
}

