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
 * Pantalla de **inicio de sesión**.
 *
 * Responsabilidades:
 * - Gestiona el formulario local (email/contraseña, mostrar/ocultar password, recordar email).
 * - Observa estado del [AuthViewModel] (usuario/logging) y **navega** cuando hay sesión.
 * - Consume eventos efímeros ([AuthEvent]) para mostrar **snackbars 1-vez**.
 * - Persiste la preferencia y valor del email mediante [UserPrefs] (DataStore).
 *
 * Decisiones de UI/UX:
 * - `Scaffold` con `SnackbarHost` para feedback no intrusivo.
 * - `imePadding()` para evitar solapamiento con el teclado en edge-to-edge.
 * - `verticalScroll` + paddings para que el contenido sea alcanzable en pantallas pequeñas.
 *
 * Accesibilidad:
 * - Título marcado con `semantics { heading() }` para lectores de pantalla.
 *
 * Navegación:
 * - Al detectar `user != null` se llama a [onAuthenticated]; el back stack se limpia aguas arriba
 *   (ver Splash/NavGraph). Aquí solo se dispara el callback.
 *
 * Notas:
 * - Se prioriza `events` frente a `error/message` del VM para evitar re-mostrados.
 * - Los textos visibles deberían estar en `strings.xml` para i18n (ver `Text(...)`).
 *
 * @param onAuthenticated Callback cuando hay usuario autenticado (incluye anónimo/verificado).
 * @param onGoToRegister Navegar a registro.
 * @param logoRes Recurso opcional de logo para la cabecera.
 * @param viewModel Inyectado con Hilt; expone estado/acciones de Auth.
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

    // VM state (lifecycle-aware).
    val user    by viewModel.user.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()

    // Estado del formulario (saveable para restaurar en rotación/proceso).
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var rememberMe by rememberSaveable { mutableStateOf(false) }

    // Host de snackbars para eventos efímeros.
    val snackbarHostState = remember { SnackbarHostState() }

    // Reglas de habilitación del botón de login.
    val canSubmit = email.isNotBlank() && password.isNotBlank() && !loading

    // Si ya hay usuario (por ejemplo, tras login/registro), navega fuera de login.
    LaunchedEffect(user?.uid) { if (user != null) onAuthenticated() }

    // Colecta de eventos efímeros del VM → snackbars one-shot (sin reemitir tras recomposición).
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

    // Preferencias/DataStore (recordar email).
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val remembered by remember { UserPrefs.rememberEmailFlow(context) }.collectAsState(initial = false)
    val savedEmail by remember { UserPrefs.savedEmailFlow(context) }.collectAsState(initial = "")

    // Sincroniza el check con el valor almacenado.
    LaunchedEffect(remembered) { rememberMe = remembered }

    // Carga el email guardado una sola vez cuando `remembered` es true.
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
                .imePadding()     // Evita que el teclado tape los campos de texto.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(36.dp))

            // Cabecera de branding
            AuthLogo(logoRes, shapes)

            // Título (accesible como encabezado)
            Text(
                text = "¡Bienvenid@! Inicia sesión y disfruta del sabor mejor desarrollado...",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )

            Spacer(Modifier.height(18.dp))

            // Campo email
            EmailField(
                value = email,
                onValueChange = { email = it },
                enabled = !loading
            )

            Spacer(Modifier.height(12.dp))

            // Campo contraseña
            PasswordField(
                value = password,
                onValueChange = { password = it },
                visible = passwordVisible,
                onToggleVisible = { passwordVisible = !passwordVisible },
                enabled = !loading,
                onDone = {
                    // Enviar con IME action (Done): cierra teclado y lanza login.
                    focus.clearFocus()
                    viewModel.signInWithEmail(email.trim(), password)
                }
            )

            Spacer(Modifier.height(10.dp))

            // "Recordar email" (persistencia en DataStore)
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


            // "He olvidado mi contraseña"
            ForgotPasswordRow(
                onClick = { viewModel.sendPasswordReset(email) },
                enabled = !loading
            )

            Spacer(Modifier.height(18.dp))

            // Botones de acción (Login / Register)
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

            // Acceso como invitado (crea sesión anónima si no existe)
            GuestAccess(
                onClick = { viewModel.signInAnonymouslyIfNeeded() },
                enabled = !loading
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}
