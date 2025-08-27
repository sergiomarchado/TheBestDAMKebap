package com.sergiom.thebestdamkebap.view.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
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

/**
 * Shell + orquestación de la sección **Home**.
 *
 * - Conecta con `AuthViewModel` y `CartViewModel` para datos globales (usuario, carrito).
 * - Escucha eventos efímeros (snackbars, ir al carrito) de `AuthViewModel` y `HomeViewModel`.
 * - Aloja el `HomeNavGraph` con un NavController **hijo** (independiente del grafo raíz).
 *
 * Nota: los efectos que colectan `Flow`s están ligados al ciclo de vida (STARTED) para
 * evitar fugas y colecciones duplicadas al recomponer.
 */
@Composable
fun HomeScreen(
    @DrawableRes logoRes: Int? = null,
    onOpenRegister: () -> Unit,
    onSignedOut: () -> Unit,
    onOpenCart: () -> Unit = {},       // analytics / navegación global (además navegamos local)
    viewModel: HomeViewModel = hiltViewModel(),
    authVm: AuthViewModel = hiltViewModel(),
    cartVm: CartViewModel = hiltViewModel()
) {
    // --- Estado Auth / Cart ---
    val user by authVm.user.collectAsStateWithLifecycle()
    val loading by authVm.loading.collectAsStateWithLifecycle()
    val cartCount by cartVm.totalItems.collectAsStateWithLifecycle()

    // Tratamos `user == null` como invitado (permite gatear en Products)
    val userIsGuest = user?.isAnonymous != false

    // Etiquetas de UI derivadas del usuario (estable ante recomposición)
    val userLabel by remember(user) {
        derivedStateOf {
            when {
                userIsGuest -> "Invitado"
                !user?.name.isNullOrBlank()  -> user?.name!!
                !user?.email.isNullOrBlank() -> user?.email!!
                else -> "Usuario"
            }
        }
    }
    val userEmail by remember(user) { derivedStateOf { user?.email.orEmpty() } }

    // --- Snackbars / eventos efímeros ---
    val snackbarHostState = remember { SnackbarHostState() }

    // Efectos lifecycle-aware para colectar eventos (evita duplicados en recomposición)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(authVm, lifecycle) {
        lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            authVm.events.collectLatest { ev ->
                when (ev) {
                    is AuthEvent.Error -> snackbarHostState.showSnackbar(ev.text)
                    is AuthEvent.Info  -> snackbarHostState.showSnackbar(ev.text)
                    else -> Unit
                }
            }
        }
    }
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            viewModel.events.collectLatest { ev ->
                when (ev) {
                    is HomeEvent.Error -> snackbarHostState.showSnackbar(ev.text)
                    is HomeEvent.Info  -> snackbarHostState.showSnackbar(ev.text)
                    HomeEvent.NavigateToCart -> {
                        onOpenCart() // callback externo (analytics/global)
                        // la navegación interna al carrito la hace el FAB via innerNav
                    }
                }
            }
        }
    }

    // --- Login dialog reusado ---
    var showLoginDialog by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(userIsGuest) { if (!userIsGuest) showLoginDialog = false }

    // Limpiar contexto de pedido al cerrar sesión
    val orderGateVm: OrderGateViewModel = hiltViewModel()

    // Guardamos el NavController interno de Home para navegar al carrito desde el FAB
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
            onOpenCart()                       // callback externo
            innerNav?.navigate(HomeRoutes.CART) // navegación local al carrito
        }
    ) { padding, navController ->
        innerNav = navController // recordamos el NavController hijo
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
        isGuest = userIsGuest, // ⬅️ NUEVO
        onDismiss = { showLoginDialog = false },
        onConfirm = { email, password -> authVm.signInWithEmail(email.trim(), password) },
        onForgot = { email -> authVm.sendPasswordReset(email) },
        onGoRegister = {
            showLoginDialog = false
            onOpenRegister()
        }
    )
}
