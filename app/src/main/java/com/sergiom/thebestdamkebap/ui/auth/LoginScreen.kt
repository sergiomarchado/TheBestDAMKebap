package com.sergiom.thebestdamkebap.ui.auth

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergiom.thebestdamkebap.data.UserPrefs
import com.sergiom.thebestdamkebap.ui.auth.components.login.*
import com.sergiom.thebestdamkebap.viewmodel.auth.AuthEvent
import com.sergiom.thebestdamkebap.viewmodel.auth.AuthViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Pantalla de Login migrada a eventos efímeros:
 * - Se elimina LaunchedEffect(error) / LaunchedEffect(message)
 * - Se añade colecta de viewModel.events para mostrar snackbars 1-sólo-vez
 */
@Composable
fun LoginScreen(
    onAuthenticated: () -> Unit = {},
    onGoToRegister: () -> Unit = {},
    @DrawableRes logoRes: Int? = null,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val colors = MaterialTheme.colorScheme
    val shapes = MaterialTheme.shapes
    val focus = LocalFocusManager.current

    // VM state
    val user    by viewModel.user.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    // NOTE: error/message siguen existiendo en VM por compatibilidad, pero aquí no se usan ya.

    // Local form
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var rememberMe by rememberSaveable { mutableStateOf(false) }

    // Snackbars
    val snackbarHostState = remember { SnackbarHostState() }

    val canSubmit = email.isNotBlank() && password.isNotBlank() && !loading

    // Navegación cuando ya hay usuario
    LaunchedEffect(user?.uid) { if (user != null) onAuthenticated() }

    // ✅ NUEVO: eventos efímeros (snackbars 1 vez)
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { ev ->
            when (ev) {
                is AuthEvent.Error -> snackbarHostState.showSnackbar(ev.text)
                is AuthEvent.Info  -> snackbarHostState.showSnackbar(ev.text)
                AuthEvent.RegisterSuccess -> Unit      // Ignorar en Login
                AuthEvent.NavigateToLogin -> Unit      // Ya estamos en Login
            }
        }
    }

    // DataStore
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val remembered by remember { UserPrefs.rememberEmailFlow(context) }.collectAsState(initial = false)
    val savedEmail by remember { UserPrefs.savedEmailFlow(context) }.collectAsState(initial = "")

    // Sync checkbox
    LaunchedEffect(remembered) { rememberMe = remembered }

    // Cargar email guardado una sola vez
    var loadedEmail by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(remembered, savedEmail) {
        if (remembered && !loadedEmail) {
            email = savedEmail
            loadedEmail = true
        }
    }

    Scaffold(
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(36.dp))

            AuthLogo(logoRes, shapes)

            Text(
                text = "¡Bienvenid@! Inicia sesión y disfruta del sabor mejor desarrollado...",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )

            Spacer(Modifier.height(18.dp))

            EmailField(
                value = email,
                onValueChange = { email = it },
                enabled = !loading
            )

            Spacer(Modifier.height(12.dp))

            PasswordField(
                value = password,
                onValueChange = { password = it },
                visible = passwordVisible,
                onToggleVisible = { passwordVisible = !passwordVisible },
                enabled = !loading,
                onDone = {
                    focus.clearFocus()
                    viewModel.signInWithEmail(email.trim(), password)
                }
            )

            Spacer(Modifier.height(10.dp))

            RememberEmailRow(
                checked = rememberMe,
                onToggle = { checked ->
                    rememberMe = checked
                    scope.launch {
                        UserPrefs.setRememberEmail(context, checked)
                        if (!checked) UserPrefs.clearSavedEmail(context)
                        else UserPrefs.setSavedEmail(context, email.trim())
                    }
                },
                enabled = !loading
            )

            Spacer(Modifier.height(8.dp))

            ForgotPasswordRow(
                onClick = { viewModel.sendPasswordReset(email) },
                enabled = !loading
            )

            Spacer(Modifier.height(18.dp))

            AuthButtonsRow(
                loading = loading,
                enabledLogin = canSubmit,
                onLogin = {
                    if (!loading && canSubmit) {
                        scope.launch {
                            if (rememberMe) UserPrefs.setSavedEmail(context, email.trim())
                            else UserPrefs.clearSavedEmail(context)
                            viewModel.signInWithEmail(email.trim(), password)
                        }
                    }
                },
                onRegister = onGoToRegister
            )

            Spacer(Modifier.height(10.dp))

            GuestAccess(
                onClick = { viewModel.signInAnonymouslyIfNeeded() },
                enabled = !loading
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}
