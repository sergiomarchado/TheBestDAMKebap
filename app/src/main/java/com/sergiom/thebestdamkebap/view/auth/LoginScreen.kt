package com.sergiom.thebestdamkebap.view.auth

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
import com.sergiom.thebestdamkebap.view.auth.components.login.AuthButtonsRow
import com.sergiom.thebestdamkebap.view.auth.components.login.AuthLogo
import com.sergiom.thebestdamkebap.view.auth.components.login.EmailField
import com.sergiom.thebestdamkebap.view.auth.components.login.ForgotPasswordRow
import com.sergiom.thebestdamkebap.view.auth.components.login.GuestAccess
import com.sergiom.thebestdamkebap.view.auth.components.login.PasswordField
import com.sergiom.thebestdamkebap.view.auth.components.login.RememberEmailRow
import com.sergiom.thebestdamkebap.viewmodel.auth.AuthEvent
import com.sergiom.thebestdamkebap.viewmodel.auth.AuthViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Pantalla de inicio de sesión.
 */
@Composable
fun LoginScreen(
    onAuthenticated: () -> Unit = {},
    onGoToRegister: () -> Unit = {},
    onContinueAsGuest: () -> Unit = {},
    @DrawableRes logoRes: Int? = null,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val colors = MaterialTheme.colorScheme
    val shapes = MaterialTheme.shapes
    val focus = LocalFocusManager.current

    // Estado del VM
    val user    by viewModel.user.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()

    // Estado del formulario
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var rememberMe by rememberSaveable { mutableStateOf(false) }

    // Snackbar + scope
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Diálogo para reemplazar sesión invitado
    var showReplaceAnonDialog by rememberSaveable { mutableStateOf(false) }
    var pendingEmail by rememberSaveable { mutableStateOf("") }
    var pendingPassword by rememberSaveable { mutableStateOf("") }

    val canSubmit = email.isNotBlank() && password.isNotBlank() && !loading
    val isAnon = user?.isAnonymous == true

    // Si ya hay usuario, navega fuera
    LaunchedEffect(user?.id) { if (user != null) onAuthenticated() }

    // Eventos del VM → snackbars
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { ev ->
            when (ev) {
                is AuthEvent.Error -> snackbarHostState.showSnackbar(ev.text)
                is AuthEvent.Info  -> snackbarHostState.showSnackbar(ev.text)
                AuthEvent.RegisterSuccess -> Unit
                AuthEvent.NavigateToLogin -> Unit
            }
        }
    }

    // Preferencias (recordar email)
    val context = LocalContext.current
    val remembered by remember { UserPrefs.rememberEmailFlow(context) }.collectAsState(initial = false)
    val savedEmail by remember { UserPrefs.savedEmailFlow(context) }.collectAsState(initial = "")

    LaunchedEffect(remembered) { rememberMe = remembered }

    var loadedEmail by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(remembered, savedEmail) {
        if (remembered && !loadedEmail) {
            email = savedEmail
            loadedEmail = true
        }
    }

    fun doLoginNow(e: String, p: String) {
        scope.launch {
            if (rememberMe) UserPrefs.setSavedEmail(context, e) else UserPrefs.clearSavedEmail(context)
            viewModel.signInWithEmail(e, p)
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
                    if (!canSubmit) return@PasswordField
                    if (isAnon) {
                        pendingEmail = email.trim()
                        pendingPassword = password
                        showReplaceAnonDialog = true
                    } else {
                        doLoginNow(email.trim(), password)
                    }
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
                        focus.clearFocus()
                        if (isAnon) {
                            pendingEmail = email.trim()
                            pendingPassword = password
                            showReplaceAnonDialog = true
                        } else {
                            doLoginNow(email.trim(), password)
                        }
                    }
                },
                onRegister = onGoToRegister
            )

            Spacer(Modifier.height(10.dp))

            GuestAccess(
                onClick = onContinueAsGuest,
                enabled = !loading
            )

            Spacer(Modifier.height(24.dp))
        }
    }

    // Confirmación: reemplazar sesión invitado
    if (showReplaceAnonDialog) {
        AlertDialog(
            onDismissRequest = { showReplaceAnonDialog = false },
            title = { Text("Cambiar de invitado a cuenta existente") },
            text = {
                Text(
                    "Vas a iniciar sesión con una cuenta.\n\n" +
                            "La sesión de invitado se reemplazará y los datos creados como invitado " +
                            "no se migrarán automáticamente."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showReplaceAnonDialog = false
                    doLoginNow(pendingEmail, pendingPassword)
                }) { Text("Continuar") }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceAnonDialog = false }) { Text("Cancelar") }
            }
        )
    }
}
