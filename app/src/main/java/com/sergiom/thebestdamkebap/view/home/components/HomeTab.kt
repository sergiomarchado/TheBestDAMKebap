package com.sergiom.thebestdamkebap.view.home.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.RestaurantMenu

internal sealed class HomeTab(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    data object Dashboard : HomeTab("home/dashboard", "Inicio", Icons.Outlined.Home)
    data object Offers    : HomeTab("home/offers",    "Ofertas", Icons.Outlined.LocalOffer)
    data object Products  : HomeTab("home/products",  "Productos", Icons.Outlined.RestaurantMenu)
}