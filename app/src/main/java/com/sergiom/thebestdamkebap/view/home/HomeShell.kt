package com.sergiom.thebestdamkebap.view.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sergiom.thebestdamkebap.navigation.HomeRoutes
import com.sergiom.thebestdamkebap.view.home.components.HomeBottomBar
import com.sergiom.thebestdamkebap.view.home.components.HomeDrawerContent
import com.sergiom.thebestdamkebap.view.home.components.HomeNavItem
import com.sergiom.thebestdamkebap.view.home.components.HomeTopBar
import kotlinx.coroutines.launch
import com.sergiom.thebestdamkebap.R

/**
 * Contenedor visual principal de **Home**.
 *
 * Estructura:
 * - Drawer lateral con opciones de cuenta.
 * - TopBar (branding + acciones de sesión).
 * - BottomBar de tabs (Inicio, Ofertas, Productos) con **save/restore** de estado entre tabs.
 * - FAB del carrito con badge de cantidad.
 * - Slot de contenido para montar el `NavHost` **interno** de Home.
 *
 * Naturaleza:
 * - Componente **presentacional**: no contiene lógica de negocio; delega callbacks.
 *
 * Previews/Tests:
 * - [navController] inyectable para facilitar pruebas y previsualización.
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
    // Inyectable para previews/tests: permite pasar un NavController de prueba.
    navController: NavHostController = rememberNavController(),
    content: @Composable (padding: PaddingValues, navController: NavHostController) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    // Tabs superiores (con i18n)
    val labelHome    = stringResource(R.string.home_tab_dashboard)
    val labelOffers  = stringResource(R.string.home_tab_offers)
    val labelProducts= stringResource(R.string.home_tab_products)

    // Tabs superiores (memoizadas para evitar recreaciones)
    val items = remember {
        listOf(
            HomeNavItem(HomeRoutes.HOME,     Icons.Outlined.Home,          labelHome),
            HomeNavItem(HomeRoutes.OFFERS,   Icons.Outlined.LocalOffer,    labelOffers),
            HomeNavItem(HomeRoutes.PRODUCTS, Icons.Outlined.RestaurantMenu,labelProducts),
        )
    }
    val topRoutes = remember { setOf(HomeRoutes.HOME, HomeRoutes.OFFERS, HomeRoutes.PRODUCTS) }

    // Ruta actual derivada del back stack → resalta tab y decide visibilidad del FAB
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute by remember(navBackStackEntry) {
        derivedStateOf {
            navBackStackEntry?.destination
                ?.hierarchy
                ?.firstOrNull { it.route in topRoutes + setOf(HomeRoutes.CART) }
                ?.route
        }
    }
    val showFab = currentRoute != HomeRoutes.CART

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            // Contenido del drawer (con colores del tema)
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
                            // Re-seleccionar tab actual → vuelve a su raíz
                            navController.popBackStack(route, false)
                            return@HomeBottomBar
                        }
                        // Cambiar de tab con preservación de estado
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            },
            floatingActionButton = {
                if (showFab) {
                    FloatingActionButton(
                        onClick = onOpenCart,
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        BadgedBox(badge = {
                            if (cartCount > 0) Badge { Text("$cartCount") }
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.ShoppingCart,
                                contentDescription =  stringResource(R.string.home_fab_cart_cd, cartCount)
                            )
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { padding ->
            content(padding, navController) // aquí montas el NavHost interno de Home
        }
    }
}
