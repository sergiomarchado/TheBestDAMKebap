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
import kotlinx.coroutines.flow.collectLatest

/**
 * Coordinador de Home: conecta VMs (Auth/Home), maneja efectos efímeros y
 * delega toda la UI de shell en [HomeShell]. Mantiene el diálogo de login aparte.
 */
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

    // --- Snackbars + efectos efímeros ---
    val snackbarHostState = remember { SnackbarHostState() }

    // Efectos de eventos (Auth)
    LaunchedEffect(Unit) {
        authVm.events.collectLatest { ev ->
            when (ev) {
                is AuthEvent.Error -> snackbarHostState.showSnackbar(ev.text)
                is AuthEvent.Info  -> snackbarHostState.showSnackbar(ev.text)
                else -> Unit
            }
        }
    }
    // Efectos de eventos (Home)
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { ev ->
            when (ev) {
                is HomeEvent.Error -> snackbarHostState.showSnackbar(ev.text)
                is HomeEvent.Info  -> snackbarHostState.showSnackbar(ev.text)
                HomeEvent.NavigateToCart -> onOpenCart()
            }
        }
    }

    // --- Diálogo de Login (para invitados) ---
    var showLoginDialog by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(userIsGuest) { if (!userIsGuest) showLoginDialog = false }

    // --- Shell + Nav interno ---
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
            onSignedOut()
        },
        onOpenCart = onOpenCart,
        // Slot de contenido: grafo interno de Home
        content = { padding, navController ->
            HomeNavGraph(
                navController = navController,
                modifier = androidx.compose.ui.Modifier.padding(padding)
            )
        }
    )

    // Diálogo de login reusado
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
