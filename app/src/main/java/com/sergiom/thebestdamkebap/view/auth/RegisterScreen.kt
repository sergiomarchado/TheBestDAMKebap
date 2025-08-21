package com.sergiom.thebestdamkebap.view.auth

import android.util.Patterns
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergiom.thebestdamkebap.view.auth.components.register.FormRegister
import com.sergiom.thebestdamkebap.view.auth.components.register.RegisterLogoAndTitle
import com.sergiom.thebestdamkebap.viewmodel.auth.AuthEvent
import com.sergiom.thebestdamkebap.viewmodel.auth.AuthViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * Pantalla de **registro** con email/contraseña.
 *
 * Responsabilidades:
 * - Gestiona estado local del formulario (nombre opcional, email, password, confirmación).
 * - Valida entrada mínima en cliente (formato email, longitud password, confirmación).
 * - Observa `loading` del [AuthViewModel] y responde a **eventos efímeros** ([AuthEvent])
 *   para mostrar snackbars y coordinar navegación post-registro.
 * - Adapta layout a pantallas anchas usando [BoxWithConstraints] (2 columnas ≥ 600dp).
 *
 * Flujo:
 * 1) Usuario envía → se marca `awaitingRegister` para prevenir reintentos rápidos.
 * 2) VM emite `RegisterSuccess` → se llama a `requestEmailVerificationAndLogout()`.
 * 3) VM emite `NavigateToLogin` → se limpia `awaitingRegister` y se llama a [onBackToLogin].
 *
 * UX/Accesibilidad:
 * - `imePadding()` en el contenedor raíz para evitar que el teclado tape campos/botones.
 * - En móviles, se añade `verticalScroll` para alcanzar todo el contenido.
 *
 * @param onBackToLogin Acción para volver a Login (el grafo ya gestiona el back stack).
 * @param logoRes Recurso opcional para branding en cabecera.
 * @param viewModel VM inyectado con Hilt que expone `loading` y `events`.
 */
@Composable
fun RegisterScreen(
    onBackToLogin: () -> Unit = {},
    @DrawableRes logoRes: Int? = null,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val colors = MaterialTheme.colorScheme
    val focus = LocalFocusManager.current

    // VM: sólo necesitamos el estado de carga; los eventos se manejan abajo.
    val loading by viewModel.loading.collectAsStateWithLifecycle()

    // Estado del formulario (saveable → sobrevive a rotaciones/proceso si es posible).
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var showPass by rememberSaveable { mutableStateOf(false) }
    var showConfirm by rememberSaveable { mutableStateOf(false) }
    var awaitingRegister by rememberSaveable { mutableStateOf(false) }

    // Validación local mínima (no sustituye a validación en servidor/VM).
    val emailError = email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val passError = password.isNotBlank() && password.length < 6
    val confirmError = confirm.isNotBlank() && confirm != password
    val mandatoryMissing = email.isBlank() || password.isBlank() || confirm.isBlank()
    val formHasErrors = emailError || passError || confirmError || mandatoryMissing
    val canSubmit = !formHasErrors && !loading && !awaitingRegister

    // Snackbars + eventos efímeros del VM.
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { ev ->
            when (ev) {
                is AuthEvent.Error -> {
                    if (awaitingRegister) awaitingRegister = false // re-habilita submit tras error
                    snackbarHostState.showSnackbar(ev.text)
                }
                is AuthEvent.Info -> {
                    snackbarHostState.showSnackbar(ev.text)
                }
                AuthEvent.RegisterSuccess -> {
                    // Al registrarse: solicitar verificación y hacer logout (flujo gestionado por VM).
                    viewModel.requestEmailVerificationAndLogout()
                }
                AuthEvent.NavigateToLogin -> {
                    awaitingRegister = false
                    onBackToLogin()
                }
            }
        }
    }

    Scaffold(
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()   // evita solapamiento con teclado
        ) {
            val isWide = maxWidth >= 600.dp
            val formMaxWidth: Dp = if (isWide) 480.dp else Dp.Unspecified

            if (isWide) {
                // Layout 2 columnas para pantallas anchas.
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Izquierda: logo + título (centrados verticalmente).
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RegisterLogoAndTitle(
                            logoRes = logoRes,
                            title = "Crea tu cuenta y únete a The Best Dönner DAM"
                        )
                    }

                    // Derecha: formulario con scroll independiente.
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FormRegister(
                            name = name, onName = { name = it },
                            email = email, onEmail = { email = it }, emailError = emailError,
                            password = password, onPassword = { password = it },
                            passVisible = showPass, onTogglePassVisible = { showPass = !showPass }, passError = passError,
                            confirm = confirm, onConfirm = { confirm = it },
                            confirmVisible = showConfirm, onToggleConfirmVisible = { showConfirm = !showConfirm }, confirmError = confirmError,
                            loading = loading,
                            enabled = canSubmit,
                            onSubmit = {
                                // Envío del formulario
                                focus.clearFocus()
                                awaitingRegister = true
                                viewModel.registerWithEmail(
                                    name = name.trim().ifEmpty { null },
                                    email = email.trim(),
                                    password = password,
                                    confirmPassword = confirm
                                )
                            },
                            formMaxWidth = formMaxWidth
                        )

                        Spacer(Modifier.height(24.dp))

                        TextButton(
                            onClick = onBackToLogin,
                            enabled = !loading && !awaitingRegister
                        ) {
                            Text("¿Ya tienes cuenta? Inicia sesión", color = colors.primary)
                        }

                        Spacer(Modifier.height(24.dp))
                    }
                }
            } else {
                // Layout móvil/vertical (scroll en toda la pantalla).
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(36.dp))

                    RegisterLogoAndTitle(
                        logoRes = logoRes,
                        title = "Crea tu cuenta y únete a The Best Dönner DAM"
                    )

                    Spacer(Modifier.height(24.dp))

                    FormRegister(
                        name = name, onName = { name = it },
                        email = email, onEmail = { email = it }, emailError = emailError,
                        password = password, onPassword = { password = it },
                        passVisible = showPass, onTogglePassVisible = { showPass = !showPass }, passError = passError,
                        confirm = confirm, onConfirm = { confirm = it },
                        confirmVisible = showConfirm, onToggleConfirmVisible = { showConfirm = !showConfirm }, confirmError = confirmError,
                        loading = loading,
                        enabled = canSubmit,
                        onSubmit = {
                            focus.clearFocus()
                            awaitingRegister = true
                            viewModel.registerWithEmail(
                                name = name.trim().ifEmpty { null },
                                email = email.trim(),
                                password = password,
                                confirmPassword = confirm
                            )
                        },
                        formMaxWidth = formMaxWidth
                    )

                    Spacer(Modifier.height(24.dp))

                    TextButton(
                        onClick = onBackToLogin,
                        enabled = !loading && !awaitingRegister
                    ) {
                        Text("¿Ya tienes cuenta? Inicia sesión", color = colors.primary)
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
