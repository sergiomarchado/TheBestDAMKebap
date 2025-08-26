package com.sergiom.thebestdamkebap.view.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.sergiom.thebestdamkebap.navigation.HomeRoutes
import com.sergiom.thebestdamkebap.view.home.components.LoginDialogIfNeeded
import com.sergiom.thebestdamkebap.viewmodel.auth.AuthEvent
import com.sergiom.thebestdamkebap.viewmodel.auth.AuthViewModel
import com.sergiom.thebestdamkebap.viewmodel.cart.CartViewModel
import com.sergiom.thebestdamkebap.viewmodel.home.HomeEvent
import com.sergiom.thebestdamkebap.viewmodel.home.HomeViewModel
import com.sergiom.thebestdamkebap.viewmodel.order.OrderGateViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HomeScreen(
    @DrawableRes logoRes: Int? = null,
    onOpenLogin: () -> Unit,           // lo seguimos exponiendo por si quieres usar pantalla dedicada
    onOpenRegister: () -> Unit,
    onSignedOut: () -> Unit,
    onOpenCart: () -> Unit = {},       // se usa (analytics / navegación global) + navegamos local
    viewModel: HomeViewModel = hiltViewModel(),
    authVm: AuthViewModel = hiltViewModel(),
    cartVm: CartViewModel = hiltViewModel()
) {
    // --- Estado Auth / Cart ---
    val user by authVm.user.collectAsStateWithLifecycle()
    val loading by authVm.loading.collectAsStateWithLifecycle()
    val cartCount by cartVm.totalItems.collectAsStateWithLifecycle()

    val userIsGuest = user?.isAnonymous != false
    val userLabel = remember(user) {
        when {
            userIsGuest -> "Invitado"
            !user?.name.isNullOrBlank()  -> user?.name!!
            !user?.email.isNullOrBlank() -> user?.email!!
            else -> "Usuario"
        }
    }
    val userEmail = remember(user) { user?.email.orEmpty() }

    // --- Snackbars / eventos efímeros ---
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
                HomeEvent.NavigateToCart -> {
                    onOpenCart() // callback externo (si quieres usar navegación global/analytics)
                    // la navegación local al carrito la gestionamos con el FAB abajo
                }
            }
        }
    }

    // --- Login dialog reusado ---
    var showLoginDialog by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(userIsGuest) { if (!userIsGuest) showLoginDialog = false }

    // limpiar contexto de pedido al cerrar sesión
    val orderGateVm: OrderGateViewModel = hiltViewModel()

    // guardaremos el NavController interno de Home para navegar al carrito desde el FAB
    var innerNav by remember { mutableStateOf<NavHostController?>(null) }

    // --- Shell + navegación interna ---
    HomeShell(
        logoRes = logoRes,
        userLabel = userLabel,
        userEmail = userEmail.takeIf { it.isNotBlank() },
        userIsGuest = userIsGuest,
        cartCount = cartCount,
        snackbarHostState = snackbarHostState,
        onOpenLogin = { showLoginDialog = true }, // usamos diálogo embebido
        onOpenRegister = {
            showLoginDialog = false
            onOpenRegister()
        },
        onSignOut = {
            authVm.signOut()
            orderGateVm.clear()
            onSignedOut()
        },
        onOpenCart = {
            onOpenCart()                      // callback externo
            innerNav?.navigate(HomeRoutes.CART) // navegación local al carrito
        }
    ) { padding, navController ->
        // guardamos el navController interno para usarlo desde el FAB
        innerNav = navController

        HomeNavGraph(
            navController = navController,
            modifier = Modifier.padding(padding),
            isGuest = userIsGuest,
            onRequestLogin = { showLoginDialog = true }, // gate pide login → abre diálogo
            onRequestRegister = onOpenRegister
        )
    }

    // Diálogo de login
    LoginDialogIfNeeded(
        show = showLoginDialog,
        loading = loading,
        onDismiss = { showLoginDialog = false },
        onConfirm = { email, password -> authVm.signInWithEmail(email.trim(), password) },
        onForgot = { email -> authVm.sendPasswordReset(email) },
        onGoRegister = {
            showLoginDialog = false
            onOpenRegister()
        }
    )
}
