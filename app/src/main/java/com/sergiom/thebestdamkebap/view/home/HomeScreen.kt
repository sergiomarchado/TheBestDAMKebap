// view/home/HomeScreen.kt
package com.sergiom.thebestdamkebap.view.home

import android.util.Patterns
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.sergiom.thebestdamkebap.view.home.components.HomeBottomBar
import com.sergiom.thebestdamkebap.view.home.components.HomeNavItem
import com.sergiom.thebestdamkebap.view.home.components.HomeTopBar
import com.sergiom.thebestdamkebap.viewmodel.auth.AuthEvent
import com.sergiom.thebestdamkebap.viewmodel.auth.AuthViewModel
import com.sergiom.thebestdamkebap.viewmodel.home.HomeEvent
import com.sergiom.thebestdamkebap.viewmodel.home.HomeViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    @DrawableRes logoRes: Int? = null,
    onOpenLogin: () -> Unit,
    onOpenRegister: () -> Unit,
    onSignedOut: () -> Unit,
    onOpenCart: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    authVm: AuthViewModel = hiltViewModel()
) {
    val colors = MaterialTheme.colorScheme
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    // Drawer state
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // Home VM
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    // Auth VM
    val user by authVm.user.collectAsStateWithLifecycle()
    val loading by authVm.loading.collectAsStateWithLifecycle()

    val userIsGuest = user == null || user!!.isAnonymous
    val userLabel = remember(user) {
        when {
            userIsGuest -> "Invitado"
            !user!!.displayName.isNullOrBlank() -> user!!.displayName!!
            !user!!.email.isNullOrBlank()       -> user!!.email!!
            else                                -> "Usuario"
        }
    }
    val userEmail = remember(user) { user?.email.orEmpty() }

    // Snackbar para eventos efímeros
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        authVm.events.collectLatest { ev ->
            when (ev) {
                is AuthEvent.Error -> snackbarHostState.showSnackbar(ev.text)
                is AuthEvent.Info  -> snackbarHostState.showSnackbar(ev.text)
                else -> Unit
            }
        }
    }
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { ev ->
            when (ev) {
                is HomeEvent.Error -> snackbarHostState.showSnackbar(ev.text)
                is HomeEvent.Info  -> snackbarHostState.showSnackbar(ev.text)
                HomeEvent.NavigateToCart -> onOpenCart()
            }
        }
    }

    // Diálogo de Login para invitados (se mantiene de antes)
    var showLoginDialog by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(userIsGuest) { if (!userIsGuest) showLoginDialog = false }

    // Bottom bar
    val items = listOf(
        HomeNavItem("home",     Icons.Outlined.Home,           "Inicio"),
        HomeNavItem("offers",   Icons.Outlined.LocalOffer,     "Ofertas"),
        HomeNavItem("products", Icons.Outlined.RestaurantMenu, "Productos"),
    )
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    // ---------- Drawer + Scaffold ----------
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))

                // Header usuario
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(userLabel, style = MaterialTheme.typography.titleMedium)
                        if (!userIsGuest && userEmail.isNotBlank()) {
                            Text(userEmail, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

                // Entradas de navegación
                val drawerItems = listOf(
                    HomeNavItem("home",     Icons.Outlined.Home,           "Inicio"),
                    HomeNavItem("offers",   Icons.Outlined.LocalOffer,     "Ofertas"),
                    HomeNavItem("products", Icons.Outlined.RestaurantMenu, "Productos"),
                )
                drawerItems.forEach { item ->
                    NavigationDrawerItem(
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(item.icon, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                Spacer(Modifier.weight(1f))
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

                // Bloque inferior contextual
                if (userIsGuest) {
                    NavigationDrawerItem(
                        label = { Text("Iniciar sesión") },
                        selected = false,
                        onClick = {
                            showLoginDialog = true
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.AutoMirrored.Outlined.Login, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text("Crear cuenta") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onOpenRegister()
                        },
                        icon = { Icon(Icons.Outlined.PersonAdd, contentDescription = null) },
                        modifier = Modifier
                            .padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                } else {
                    NavigationDrawerItem(
                        label = { Text("Cerrar sesión") },
                        selected = false,
                        onClick = {
                            authVm.signOut()
                            scope.launch { drawerState.close() }
                            onSignedOut()
                        },
                        icon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    ) {
        Scaffold(
            containerColor = colors.background,
            topBar = {
                HomeTopBar(
                    logoRes = logoRes,
                    userLabel = userLabel,
                    userIsGuest = userIsGuest,
                    onOpenLogin = { showLoginDialog = true },   // píldora
                    onOpenRegister = {
                        showLoginDialog = false
                        onOpenRegister()
                    },
                    onSignOut = {
                        authVm.signOut()
                        onSignedOut()
                    },
                    onMenuClick = { scope.launch { drawerState.open() } } // hamburguesa -> abre drawer
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
                        if (ui.cartCount > 0) Badge { Text("${ui.cartCount}") }
                    }) {
                        Icon(Icons.Outlined.ShoppingCart, contentDescription = "Carrito")
                    }
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                composable("home")     { /* TODO: contenido real */ }
                composable("offers")   { /* TODO: contenido real */ }
                composable("products") { /* TODO: contenido real */ }
            }
        }
    }

    // ---------- Diálogo de Login (igual que antes) ----------
    if (showLoginDialog) {
        var email by rememberSaveable { mutableStateOf("") }
        var password by rememberSaveable { mutableStateOf("") }
        var showPass by rememberSaveable { mutableStateOf(false) }

        val emailError = email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()
        val passError  = password.isNotBlank() && password.length < 6
        val formInvalid = email.isBlank() || password.isBlank() || emailError || passError

        AlertDialog(
            onDismissRequest = { if (!loading) showLoginDialog = false },
            title = { Text("Iniciar sesión") },
            text = {
                Column {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        isError = emailError
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        singleLine = true,
                        isError = passError,
                        visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { showPass = !showPass }) {
                                Text(if (showPass) "Ocultar" else "Ver")
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { authVm.sendPasswordReset(email) },
                        enabled = email.isNotBlank()
                    ) {
                        Text("¿Olvidaste la contraseña?")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { authVm.signInWithEmail(email.trim(), password) },
                    enabled = !formInvalid && !loading
                ) {
                    if (loading) CircularProgressIndicator(strokeWidth = 2.dp) else Text("Iniciar sesión")
                }
            },
            dismissButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { if (!loading) showLoginDialog = false }
                    ) { Text("Cancelar") }
                    Spacer(Modifier.width(4.dp))
                    FilledTonalButton(
                        onClick = {
                            showLoginDialog = false
                            onOpenRegister()
                        }
                    ) { Text("Crear cuenta") }
                }
            }
        )
    }
}
