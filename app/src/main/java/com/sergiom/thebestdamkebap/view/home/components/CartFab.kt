// view/home/components/CartFab.kt
package com.sergiom.thebestdamkebap.view.home.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable

@Composable
fun CartFab(
    count: Int,
    onClick: () -> Unit
) {
    FloatingActionButton(onClick = onClick) {
        BadgedBox(
            badge = {
                if (count > 0) {
                    Badge { androidx.compose.material3.Text(text = count.coerceAtMost(99).toString()) }
                }
            }
        ) {
            Icon(Icons.Outlined.ShoppingCart, contentDescription = "Carrito")
        }
    }
}
