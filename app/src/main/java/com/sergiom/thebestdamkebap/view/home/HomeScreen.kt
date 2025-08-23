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
 * Pantalla **Home** (coordinador).
 *
 * Qué hace:
 * - Conecta los estados de [HomeViewModel] y [AuthViewModel].
 * - Gestiona mensajes puntuales (snackbars) y navegación al carrito.
 * - Muestra un diálogo de login cuando el usuario es invitado.
 * - Delega el “esqueleto” visual en [HomeShell] y el grafo interno en [HomeNavGraph].
 *
 * Por qué así:
 * - La UI no conoce detalles de datos; solo observa `ui`/`events` de los VMs.
 * - Los efectos de una sola vez (snackbars, navegar) se recogen con `LaunchedEffect(Unit)`.
 *
 * Callbacks de navegación (proveídos por el grafo superior):
 * - [onOpenLogin], [onOpenRegister], [onSignedOut], [onOpenCart].
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

    // ¿Es invitado? (true cuando no hay usuario o es anónimo)
    val userIsGuest = user == null || user!!.isAnonymous

    // Etiqueta visible del usuario (sin nullables forzados)
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

    // Eventos de Auth → snackbars informativos/errores
    LaunchedEffect(Unit) {
        authVm.events.collectLatest { ev ->
            when (ev) {
                is AuthEvent.Error -> snackbarHostState.showSnackbar(ev.text)
                is AuthEvent.Info  -> snackbarHostState.showSnackbar(ev.text)
                else -> Unit
            }
        }
    }
    // Eventos de Home → snackbars y navegación a carrito
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { ev ->
            when (ev) {
                is HomeEvent.Error -> snackbarHostState.showSnackbar(ev.text)
                is HomeEvent.Info  -> snackbarHostState.showSnackbar(ev.text)
                HomeEvent.NavigateToCart -> onOpenCart()
            }
        }
    }

    // --- Diálogo de Login (solo para invitados) ---
    var showLoginDialog by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(userIsGuest) { if (!userIsGuest) showLoginDialog = false }

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
            onSignedOut()
        },
        onOpenCart = onOpenCart,
        // Contenido principal: grafo interno de Home
        content = { padding, navController ->
            HomeNavGraph(
                navController = navController,
                modifier = androidx.compose.ui.Modifier.padding(padding)
            )
        }
    )

    // Diálogo reutilizable para iniciar sesión desde Home
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
