package com.sergiom.thebestdamkebap.view.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sergiom.thebestdamkebap.view.home.components.HomeBottomBar
import com.sergiom.thebestdamkebap.view.home.components.HomeDrawerContent
import com.sergiom.thebestdamkebap.view.home.components.HomeNavItem
import com.sergiom.thebestdamkebap.view.home.components.HomeTopBar
import com.sergiom.thebestdamkebap.view.home.components.ManageAddressesSheet
import kotlinx.coroutines.launch

/**
 * Contenedor visual de Home: Drawer (cuenta) + Scaffold (Top/Bottom bar, FAB, Snackbars)
 * + Nav interno. Maneja la apertura del sheet de **direcciones**.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeShell(
    @DrawableRes logoRes: Int?,
    userLabel: String,
    userEmail: String?,
    userIsGuest: Boolean,
    cartCount: Int,
    snackbarHostState: SnackbarHostState,
    onOpenLogin: () -> Unit,
    onOpenRegister: () -> Unit,
    onSignOut: () -> Unit,
    onOpenCart: () -> Unit,
    content: @Composable (padding: PaddingValues, navController: NavHostController) -> Unit
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // Items del bottom bar (navegación interna de exploración)
    val items = listOf(
        HomeNavItem(HomeRoutes.HOME,     Icons.Outlined.Home,           "Inicio"),
        HomeNavItem(HomeRoutes.OFFERS,   Icons.Outlined.LocalOffer,     "Ofertas"),
        HomeNavItem(HomeRoutes.PRODUCTS, Icons.Outlined.RestaurantMenu, "Productos"),
    )
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    // Sheet de direcciones
    var showAddressesSheet by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                HomeDrawerContent(
                    userLabel = userLabel,
                    userEmail = userEmail,
                    userIsGuest = userIsGuest,
                    onOpenProfile = {
                        navController.navigate(HomeRoutes.PROFILE) {
                            launchSingleTop = true
                        }
                        scope.launch { drawerState.close() }
                    },
                    onManageAddresses = {
                        showAddressesSheet = true
                        scope.launch { drawerState.close() }
                    },
                    onOpenOrders = {
                        navController.navigate(HomeRoutes.ORDERS) { launchSingleTop = true }
                        scope.launch { drawerState.close() }
                    },
                    onOpenSettings = {
                        navController.navigate(HomeRoutes.SETTINGS) { launchSingleTop = true }
                        scope.launch { drawerState.close() }
                    },
                    onLogin = {
                        onOpenLogin()
                        scope.launch { drawerState.close() }
                    },
                    onRegister = {
                        onOpenRegister()
                        scope.launch { drawerState.close() }
                    },
                    onLogout = {
                        onSignOut()
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                HomeTopBar(
                    logoRes = logoRes,
                    userLabel = userLabel,
                    userIsGuest = userIsGuest,
                    onOpenLogin = onOpenLogin,
                    onOpenRegister = onOpenRegister,
                    onSignOut = onSignOut,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            },
            bottomBar = {
                HomeBottomBar(
                    items = items,
                    currentRoute = currentRoute,
                    onItemClick = { route ->
                        if (route == currentRoute) return@HomeBottomBar
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onOpenCart) {
                    BadgedBox(badge = {
                        if (cartCount > 0) Badge { Text("$cartCount") }
                    }) {
                        Icon(Icons.Outlined.ShoppingCart, contentDescription = "Carrito")
                    }
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { padding ->
            content(padding, navController)
        }
    }

    // Sheet (modal) para gestionar direcciones
    ManageAddressesSheet(
        show = showAddressesSheet,
        onDismiss = { showAddressesSheet = false },
        addresses = emptyList(), // TODO: inyectar desde VM cuando lo tengas
        onAddAddress = { /* TODO: abrir form de nueva dirección */ },
        onEditAddress = { /* TODO: abrir form con datos */ },
        onDeleteAddress = { /* TODO: pedir confirmación y borrar */ }
    )
}
