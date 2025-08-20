package com.sergiom.thebestdamkebap.ui.auth

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergiom.thebestdamkebap.auth.AuthViewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import com.sergiom.thebestdamkebap.data.UserPrefs
import kotlinx.coroutines.launch

/**
 * Pantalla de Login (email/contraseña) con:
 * - Acceso como invitado (anónimo).
 * - Enlace “¿Olvidaste la contraseña?”.
 * - Callback [onAuthenticated] para navegar cuando ya haya usuario.
 * - Callback [onGoToRegister] para ir a la pantalla de registro.
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


    // VM
    val user    by viewModel.user.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error   by viewModel.error.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    // Form
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var rememberMe by rememberSaveable { mutableStateOf(false) } // luego lo usaremos como "Recordar email"

    // Snackbars
    val snackbarHostState = remember { SnackbarHostState() }

    // Navegación al estar autenticado
    LaunchedEffect(user?.uid) { if (user != null) onAuthenticated() }

    // Feedback
    LaunchedEffect(error)   { val msg = error   ?: return@LaunchedEffect; snackbarHostState.showSnackbar(msg) }
    LaunchedEffect(message) { val msg = message ?: return@LaunchedEffect; snackbarHostState.showSnackbar(msg) }

    val focus = LocalFocusManager.current

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Leemos preferencias
    val remembered by remember { UserPrefs.rememberEmailFlow(context) }.collectAsState(initial = false)
    val savedEmail by remember { UserPrefs.savedEmailFlow(context) }.collectAsState(initial = "")

    // Sincronizamos el checkbox con lo guardado
    LaunchedEffect(remembered) { rememberMe = remembered }

    // Cargamos el email guardado solo una vez
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

            // Logo (opcional)
            if (logoRes != null) {
                Image(
                    painter = painterResource(id = logoRes),
                    contentDescription = "Logo de DAM Burger", // a11y
                    modifier = Modifier
                        .size(160.dp)
                        .clip(shapes.medium)
                )
                Spacer(Modifier.height(12.dp))
            } else {
                Spacer(Modifier.height(90.dp))
            }

            // Título / encabezado (anunciado por TalkBack como heading)
            Text(
                text = "¡Bienvenid@! Inicia sesión y disfruta del sabor mejor desarrollado...",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() } // a11y
            )

            Spacer(Modifier.height(18.dp))

            // ===== Email =====
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                singleLine = true,
                label = { Text("Email") }, // el label ya es leído por TalkBack
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = Color.Black) },
                shape = shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
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
                    unfocusedLeadingIconColor = Color.Black
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focus.clearFocus() }),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // ===== Password =====
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                singleLine = true,
                label = { Text("Contraseña") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.Black) },
                trailingIcon = {
                    val cd = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        modifier = Modifier.semantics { contentDescription = cd } // a11y: describe la acción
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null, // lo anuncia el IconButton
                            tint = colors.primary
                        )
                    }
                },
                visualTransformation = if (passwordVisible)
                    androidx.compose.ui.text.input.VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                shape = shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
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
                    unfocusedLeadingIconColor = Color.Black
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focus.clearFocus()
                        viewModel.signInWithEmail(email.trim(), password)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            // Recordar email (antes “Recordar contraseña”) — merge para que TalkBack lea junto
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = rememberMe,
                        role = Role.Checkbox,
                        onValueChange = { checked ->
                            rememberMe = checked
                            scope.launch {
                                UserPrefs.setRememberEmail(context, checked)
                                if (!checked) UserPrefs.clearSavedEmail(context)
                                else UserPrefs.setSavedEmail(context, email.trim())
                            }
                        }
                    )
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = null, // controlado por el Row.toggleable
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                )
                Text(
                    text = "Recordar email",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(8.dp))

            // ¿Olvidaste la contraseña?
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "¿Has olvidado la contraseña? ",
                    color = colors.onBackground.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(onClick = { viewModel.sendPasswordReset(email) }) {
                    Text("Pulse aquí", color = colors.primary, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(18.dp))

            // Botones
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            if (rememberMe) UserPrefs.setSavedEmail(context, email.trim())
                            else UserPrefs.clearSavedEmail(context)
                            viewModel.signInWithEmail(email.trim(), password)
                        }
                    },
                    enabled = !loading,
                    shape = shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp) // touch target >= 48dp
                ) {
                    if (loading) CircularProgressIndicator(strokeWidth = 2.dp, color = colors.onPrimary)
                    else Text("INICIAR SESIÓN", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onGoToRegister,
                    enabled = !loading,
                    shape = shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("REGISTRAR", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(10.dp))

            // Invitado
            TextButton(onClick = { viewModel.signInAnonymouslyIfNeeded() }, enabled = !loading) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = colors.primary)
                Spacer(Modifier.width(6.dp))
                Text("Acceder como invitado", color = colors.primary)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
