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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sergiom.thebestdamkebap.view.home.components.HomeBottomBar
import com.sergiom.thebestdamkebap.view.home.components.HomeDrawerContent
import com.sergiom.thebestdamkebap.view.home.components.HomeNavItem
import com.sergiom.thebestdamkebap.view.home.components.HomeTopBar
import kotlinx.coroutines.launch

/**
 * Contenedor visual de Home: Drawer + Scaffold + Nav interno (exploración).
 * “Mis direcciones” navega a la lista integrada en el grafo interno.
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

    // Tabs del BottomBar
    val items = listOf(
        HomeNavItem(HomeRoutes.HOME, Icons.Outlined.Home, "Inicio"),
        HomeNavItem(HomeRoutes.OFFERS, Icons.Outlined.LocalOffer, "Ofertas"),
        HomeNavItem(HomeRoutes.PRODUCTS, Icons.Outlined.RestaurantMenu, "Productos"),
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val topRoutes = remember { setOf(HomeRoutes.HOME, HomeRoutes.OFFERS, HomeRoutes.PRODUCTS) }
    val currentRoute = navBackStackEntry?.destination
        ?.hierarchy
        ?.firstOrNull { it.route in topRoutes }
        ?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.tertiary,
                drawerContentColor = MaterialTheme.colorScheme.onTertiary
            ) {
                HomeDrawerContent(
                    userLabel = userLabel,
                    userEmail = userEmail,
                    userIsGuest = userIsGuest,
                    onOpenProfile = {
                        scope.launch { drawerState.close() }
                        navController.navigate(HomeRoutes.PROFILE) { launchSingleTop = true }
                    },
                    onManageAddresses = {
                        scope.launch { drawerState.close() }
                        navController.navigate(HomeRoutes.ADDRESSES) { launchSingleTop = true }
                    },
                    onOpenOrders = {
                        scope.launch { drawerState.close() }
                        navController.navigate(HomeRoutes.ORDERS) { launchSingleTop = true }
                    },
                    onOpenSettings = {
                        scope.launch { drawerState.close() }
                        navController.navigate(HomeRoutes.SETTINGS) { launchSingleTop = true }
                    },
                    onLogin = {
                        scope.launch { drawerState.close() }
                        onOpenLogin()
                    },
                    onRegister = {
                        scope.launch { drawerState.close() }
                        onOpenRegister()
                    },
                    onLogout = {
                        scope.launch { drawerState.close() }
                        onSignOut()
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
                        if (route == currentRoute) {
                            // Re-selección: volver a la raíz de ese tab
                            navController.popBackStack(route, false)
                            return@HomeBottomBar
                        }
                        // Cambio de tab: limpiar hasta el startDest y NO restaurar estado de tabs anteriores
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = false
                                saveState = false
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onOpenCart,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
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
}
