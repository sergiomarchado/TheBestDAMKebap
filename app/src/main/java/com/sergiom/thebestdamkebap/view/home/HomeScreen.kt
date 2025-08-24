package com.sergiom.thebestdamkebap.view.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergiom.thebestdamkebap.view.home.components.LoginDialogIfNeeded
import com.sergiom.thebestdamkebap.viewmodel.auth.AuthEvent
import com.sergiom.thebestdamkebap.viewmodel.auth.AuthViewModel
import com.sergiom.thebestdamkebap.viewmodel.home.HomeEvent
import com.sergiom.thebestdamkebap.viewmodel.home.HomeViewModel
import com.sergiom.thebestdamkebap.viewmodel.order.OrderGateViewModel
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.Modifier

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
    // --- Estado de VMs ---
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val user by authVm.user.collectAsStateWithLifecycle()
    val loading by authVm.loading.collectAsStateWithLifecycle()

    // ¿Es invitado? (null o anónimo)
    val userIsGuest = user?.isAnonymous != false

    // Etiquetas visibles
    val userLabel = remember(user) {
        when {
            userIsGuest -> "Invitado"
            !user?.name.isNullOrBlank()  -> user?.name!!
            !user?.email.isNullOrBlank() -> user?.email!!
            else -> "Usuario"
        }
    }
    val userEmail = remember(user) { user?.email.orEmpty() }

    // --- Snackbars + efectos efímeros ---
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

    // --- Diálogo de Login (reutilizable) ---
    var showLoginDialog by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(userIsGuest) { if (!userIsGuest) showLoginDialog = false }

    // Para limpiar la sesión de pedido al cerrar sesión
    val orderGateVm: OrderGateViewModel = hiltViewModel()

    // --- Shell + Navegación interna de Home ---
    HomeShell(
        logoRes = logoRes,
        userLabel = userLabel,
        userEmail = userEmail.takeIf { it.isNotBlank() },
        userIsGuest = userIsGuest,
        cartCount = ui.cartCount,
        snackbarHostState = snackbarHostState,
        onOpenLogin = { showLoginDialog = true },
        onOpenRegister = {
            showLoginDialog = false
            onOpenRegister()
        },
        onSignOut = {
            authVm.signOut()
            orderGateVm.clear()   // limpia contexto del pedido
            onSignedOut()
        },
        onOpenCart = onOpenCart
    ) { padding, navController ->
        HomeNavGraph(
            navController = navController,
            modifier = Modifier.padding(padding),
            isGuest = userIsGuest,
            onRequestLogin = { showLoginDialog = true }, // el gate pedirá abrir este diálogo
            onRequestRegister = onOpenRegister          // o llevar a registro
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
