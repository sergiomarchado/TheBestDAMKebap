package com.sergiom.thebestdamkebap.ui.auth

import android.util.Patterns
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergiom.thebestdamkebap.auth.AuthViewModel

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit = {},
    onBackToLogin: () -> Unit = {},
    @DrawableRes logoRes: Int? = null,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val colors = MaterialTheme.colorScheme
    val shapes = MaterialTheme.shapes

    // VM
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error   by viewModel.error.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    // Form
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

    // Snackbars
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(error)   { val msg = error   ?: return@LaunchedEffect; snackbarHostState.showSnackbar(msg) }
    LaunchedEffect(message, awaitingRegister) {
        val msg = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        if (awaitingRegister) {
            awaitingRegister = false
            onRegistered()
        }
    }

    // Colores de campos
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colors.primary,
        unfocusedBorderColor = colors.primary,
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        focusedLabelColor = Color.DarkGray,
        unfocusedLabelColor = Color.Gray,
        cursorColor = colors.primary,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedLeadingIconColor = Color.Black,
        unfocusedLeadingIconColor = Color.Black,
        focusedPlaceholderColor = Color.Gray,
        unfocusedPlaceholderColor = Color.Gray
    )

    Scaffold(
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->

        // ── Layout adaptativo: columna (compacto) vs. dos columnas (≥600dp) ──
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
                    // Lado izquierdo: logo + título (centrados)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LogoAndTitle(logoRes = logoRes, colors = colors, shapes = shapes)
                    }

                    // Lado derecho: formulario (scroll independiente)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FormContents(
                            name = name,
                            onName = { name = it },
                            email = email,
                            onEmail = { email = it },
                            emailError = emailError,
                            password = password,
                            onPassword = { password = it },
                            passVisible = showPass,
                            onTogglePassVisible = { showPass = !showPass },
                            passError = passError,
                            confirm = confirm,
                            onConfirm = { confirm = it },
                            confirmVisible = showConfirm,
                            onToggleConfirmVisible = { showConfirm = !showConfirm },
                            confirmError = confirmError,
                            fieldColors = fieldColors,
                            loading = loading,
                            enabled = !formHasErrors && !loading,
                            onSubmit = {
                                awaitingRegister = true
                                viewModel.registerWithEmail(
                                    name = name.trim().ifEmpty { null },
                                    email = email.trim(),
                                    password = password,
                                    confirmPassword = confirm
                                )
                            },
                            formMaxWidth = formMaxWidth,
                            colors = colors,
                            shapes = shapes
                        )

                        Spacer(Modifier.height(24.dp))

                        TextButton(onClick = onBackToLogin, enabled = !loading) {
                            Text("¿Ya tienes cuenta? Inicia sesión", color = colors.primary)
                        }

                        Spacer(Modifier.height(24.dp))
                    }
                }
            } else {
                // Compacto (móvil/vertical): columna única (con scroll)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(36.dp))

                    LogoAndTitle(logoRes = logoRes, colors = colors, shapes = shapes)

                    Spacer(Modifier.height(24.dp))

                    FormContents(
                        name = name,
                        onName = { name = it },
                        email = email,
                        onEmail = { email = it },
                        emailError = emailError,
                        password = password,
                        onPassword = { password = it },
                        passVisible = showPass,
                        onTogglePassVisible = { showPass = !showPass },
                        passError = passError,
                        confirm = confirm,
                        onConfirm = { confirm = it },
                        confirmVisible = showConfirm,
                        onToggleConfirmVisible = { showConfirm = !showConfirm },
                        confirmError = confirmError,
                        fieldColors = fieldColors,
                        loading = loading,
                        enabled = !formHasErrors && !loading,
                        onSubmit = {
                            awaitingRegister = true
                            viewModel.registerWithEmail(
                                name = name.trim().ifEmpty { null },
                                email = email.trim(),
                                password = password,
                                confirmPassword = confirm
                            )
                        },
                        formMaxWidth = formMaxWidth,
                        colors = colors,
                        shapes = shapes
                    )

                    Spacer(Modifier.height(24.dp))

                    TextButton(onClick = onBackToLogin, enabled = !loading) {
                        Text("¿Ya tienes cuenta? Inicia sesión", color = colors.primary)
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

/* ───────────────────── Helpers dentro del mismo archivo ───────────────────── */

@Composable
private fun LogoAndTitle(
    @DrawableRes logoRes: Int?,
    colors: ColorScheme,
    shapes: Shapes
) {
    if (logoRes != null) {
        Image(
            painter = painterResource(id = logoRes),
            contentDescription = "Logo de DAM Burger",
            modifier = Modifier
                .size(160.dp)
                .clip(shapes.medium)
        )
        Spacer(Modifier.height(12.dp))
    } else {
        Spacer(Modifier.height(90.dp))
    }

    Text(
        "Crea tu cuenta y únete a The Best Dönner DAM",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = colors.primary,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics { heading() }
    )
}

@Composable
private fun FormContents(
    name: String,
    onName: (String) -> Unit,
    email: String,
    onEmail: (String) -> Unit,
    emailError: Boolean,
    password: String,
    onPassword: (String) -> Unit,
    passVisible: Boolean,
    onTogglePassVisible: () -> Unit,
    passError: Boolean,
    confirm: String,
    onConfirm: (String) -> Unit,
    confirmVisible: Boolean,
    onToggleConfirmVisible: () -> Unit,
    confirmError: Boolean,
    fieldColors: TextFieldColors,
    loading: Boolean,
    enabled: Boolean,
    onSubmit: () -> Unit,
    formMaxWidth: Dp,
    colors: ColorScheme,
    shapes: Shapes
) {
    // Nombre (opcional)
    OutlinedTextField(
        value = name,
        onValueChange = onName,
        label = { Text("Introduzca su nombre (opcional)") },
        singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = Color.Black) },
        shape = shapes.medium,
        colors = fieldColors,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = formMaxWidth)
    )

    Spacer(Modifier.height(12.dp))

    // Email
    OutlinedTextField(
        value = email,
        onValueChange = onEmail,
        label = { Text("Introduzca su email") },
        singleLine = true,
        isError = emailError,
        supportingText = { if (emailError) Text("Introduce un email válido") },
        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = Color.Black) },
        shape = shapes.medium,
        colors = fieldColors,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        ),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = formMaxWidth)
    )

    Spacer(Modifier.height(12.dp))

    // Contraseña
    OutlinedTextField(
        value = password,
        onValueChange = onPassword,
        label = { Text("Introduzca contraseña") },
        singleLine = true,
        isError = passError,
        supportingText = { if (passError) Text("La contraseña debe tener al menos 6 caracteres") },
        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.Black) },
        trailingIcon = {
            val cd = if (passVisible) "Ocultar contraseña" else "Mostrar contraseña"
            IconButton(onClick = onTogglePassVisible) {
                Icon(
                    imageVector = if (passVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = cd,
                    tint = colors.primary
                )
            }
        },
        visualTransformation = if (passVisible)
            androidx.compose.ui.text.input.VisualTransformation.None
        else
            PasswordVisualTransformation(),
        shape = shapes.medium,
        colors = fieldColors,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next
        ),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = formMaxWidth)
    )

    Spacer(Modifier.height(12.dp))

    // Confirmación
    OutlinedTextField(
        value = confirm,
        onValueChange = onConfirm,
        label = { Text("Repita la contraseña") },
        singleLine = true,
        isError = confirmError,
        supportingText = { if (confirmError) Text("Las contraseñas no coinciden") },
        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.Black) },
        trailingIcon = {
            val cd = if (confirmVisible) "Ocultar contraseña" else "Mostrar contraseña"
            IconButton(onClick = onToggleConfirmVisible) {
                Icon(
                    imageVector = if (confirmVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = cd,
                    tint = colors.primary
                )
            }
        },
        visualTransformation = if (confirmVisible)
            androidx.compose.ui.text.input.VisualTransformation.None
        else
            PasswordVisualTransformation(),
        shape = shapes.medium,
        colors = fieldColors,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = formMaxWidth)
    )

    Spacer(Modifier.height(20.dp))

    // Botón Registrar
    Button(
        onClick = onSubmit,
        enabled = enabled,
        shape = shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = formMaxWidth)
            .height(48.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(strokeWidth = 2.dp, color = colors.onPrimary)
        } else {
            Text("REGISTRAR", fontWeight = FontWeight.Bold)
        }
    }
}
