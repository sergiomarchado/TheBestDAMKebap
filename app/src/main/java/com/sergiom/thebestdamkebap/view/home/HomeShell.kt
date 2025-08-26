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
import com.sergiom.thebestdamkebap.navigation.HomeRoutes
import com.sergiom.thebestdamkebap.view.home.components.HomeBottomBar
import com.sergiom.thebestdamkebap.view.home.components.HomeDrawerContent
import com.sergiom.thebestdamkebap.view.home.components.HomeNavItem
import com.sergiom.thebestdamkebap.view.home.components.HomeTopBar
import kotlinx.coroutines.launch

/**
 * # HomeShell
 *
 * Contenedor visual principal de la sección **Home**.
 *
 * Incluye:
 * - **Drawer** lateral con opciones de cuenta (perfil, direcciones, pedidos, ajustes).
 * - **TopBar** con branding y acciones de sesión.
 * - **BottomBar** con las tabs principales (Inicio, Ofertas, Productos).
 * - **FAB** del carrito con badge (contador).
 * - **SnackbarHost** para mensajes.
 *
 * Diseño:
 * - Este componente **no conoce** lógica de negocio: recibe callbacks y datos ya preparados
 *   (MVVM). El contenido dinámico se inyecta por `content(padding, navController)`.
 *
 * Navegación/tabs:
 * - Cuando se re-selecciona el tab actual, se hace `popBackStack(route, false)` para volver
 *   a la raíz de esa pestaña.
 * - Al cambiar de tab, se navega con `popUpTo(findStartDestination()) + save/restoreState`
 *   para conservar el estado de listas/filtros por pestaña.
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
    // Inyectable para previews/tests (permite pasar un navController de prueba)
    navController: NavHostController = rememberNavController(),
    content: @Composable (padding: PaddingValues, navController: NavHostController) -> Unit,

) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    /* ───────────────── Tabs de la bottom bar ─────────────────
     * Se memorizan para evitar recreaciones constantes en recomposición.
     */
    val items = remember {
        listOf(
            HomeNavItem(HomeRoutes.HOME, Icons.Outlined.Home, "Inicio"),
            HomeNavItem(HomeRoutes.OFFERS, Icons.Outlined.LocalOffer, "Ofertas"),
            HomeNavItem(HomeRoutes.PRODUCTS, Icons.Outlined.RestaurantMenu, "Productos"),
        )
    }
    // Conjunto de rutas "top" (sirve para detectar qué tab está activa)
    val topRoutes = remember { setOf(HomeRoutes.HOME, HomeRoutes.OFFERS, HomeRoutes.PRODUCTS) }

    // Ruta actual (solo entre las top) para resaltar tab y gestionar re-selección
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    // calcula la ruta "top" actual
    val currentRoute = navBackStackEntry?.destination
        ?.hierarchy
        ?.firstOrNull { it.route in topRoutes + setOf(HomeRoutes.CART) }
        ?.route

    // MUESTRA el FAB salvo en la pantalla del carrito
    val showFab = currentRoute != HomeRoutes.CART

    /* ───────────────── Drawer + Scaffold ───────────────── */

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            // Hoja del drawer con colores del tema (tertiary para diferenciarla)
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.tertiary,
                drawerContentColor = MaterialTheme.colorScheme.onTertiary
            ) {
                HomeDrawerContent(
                    userLabel = userLabel,
                    userEmail = userEmail,
                    userIsGuest = userIsGuest,
                    // Cada acción del menú cierra primero el drawer y luego navega/lanza callback
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
                            // Re-selección del mismo tab: vuelve a su raíz
                            navController.popBackStack(route, false)
                            return@HomeBottomBar
                        }
                        // Cambio de tab:
                        // - Limpia hasta el start destination del grafo actual
                        // - Guarda/restaura estado para conservar scroll/filtros por tab
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = false
                                saveState = true   // conserva el estado del tab al salir
                            }
                            launchSingleTop = true
                            restoreState = true // restaura el estado si vuelves al tab
                        }
                    }
                )
            },
            floatingActionButton = {
                if(showFab){
                    FloatingActionButton(
                        onClick = onOpenCart,
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        // Badge solo si hay artículos en carrito
                        BadgedBox(badge = {
                            if (cartCount > 0) Badge { Text("$cartCount") }
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.ShoppingCart,
                                // A11y: ideal mover a strings.xml
                                contentDescription = "Carrito ($cartCount)"
                            )
                        }
                    }

                }


            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { padding ->
            // Slot de contenido: aquí se monta el NavHost interno de Home
            content(padding, navController)
        }
    }
}
