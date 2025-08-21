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

@Composable
fun RegisterScreen(
    onBackToLogin: () -> Unit = {},
    @DrawableRes logoRes: Int? = null,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val colors = MaterialTheme.colorScheme
    val focus = LocalFocusManager.current

    // VM
    val loading by viewModel.loading.collectAsStateWithLifecycle()

    // Form state
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var showPass by rememberSaveable { mutableStateOf(false) }
    var showConfirm by rememberSaveable { mutableStateOf(false) }
    var awaitingRegister by rememberSaveable { mutableStateOf(false) }

    // Validación local
    val emailError = email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val passError = password.isNotBlank() && password.length < 6
    val confirmError = confirm.isNotBlank() && confirm != password
    val mandatoryMissing = email.isBlank() || password.isBlank() || confirm.isBlank()
    val formHasErrors = emailError || passError || confirmError || mandatoryMissing
    val canSubmit = !formHasErrors && !loading && !awaitingRegister

    // Snackbars + eventos efímeros
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { ev ->
            when (ev) {
                is AuthEvent.Error -> {
                    if (awaitingRegister) awaitingRegister = false
                    snackbarHostState.showSnackbar(ev.text)
                }
                is AuthEvent.Info -> {
                    snackbarHostState.showSnackbar(ev.text)
                }
                AuthEvent.RegisterSuccess -> {
                    // La VM completará: verificación + logout + NavigateToLogin
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
                .imePadding()
        ) {
            val isWide = maxWidth >= 600.dp
            val formMaxWidth: Dp = if (isWide) 480.dp else Dp.Unspecified

            if (isWide) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Izquierda: logo + título
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

                    // Derecha: formulario con scroll
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
                // Móvil/vertical
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
