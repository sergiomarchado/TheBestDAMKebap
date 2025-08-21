package com.sergiom.thebestdamkebap.viewmodel.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * **AuthViewModel**
 *
 * Orquesta el **estado de autenticación** y expone una API reactiva para la UI.
 *
 * ### Estado expuesto
 * - [user]: `FirebaseUser?` actual (incluye anónimo / verificado / no verificado).
 * - [loading]: bandera de operación en curso (login, registro, reset, etc.).
 * - [error] / [message]: transitorios para feedback en UI (consumibles).
 * - [events]: `SharedFlow` de eventos efímeros (snackbars, navegación).
 *
 * ### Acciones principales
 * - `signInAnonymouslyIfNeeded()`: arranque sin fricción si no hay sesión.
 * - `signInWithEmail(email, password)`: login con verificación de email obligatoria.
 * - `registerWithEmail(name, email, password, confirmPassword)`: registro (linkea si venías como invitado).
 * - `sendPasswordReset(email)`: email de restablecimiento.
 * - `signOut()`: cierra sesión.
 * - `requestEmailVerificationAndLogout()`: envía verificación y saca a login.
 *
 * ### Decisiones de diseño
 * - **Listener de Auth**: se mantiene un [FirebaseAuth.AuthStateListener] para reflejar cambios
 *   del SDK en [_user]; se añade en `init` y se quita en `onCleared()`.
 * - **Errores presentables**: `Throwable.toUserMessage()` mapea códigos de `FirebaseAuthException`
 *   a textos de usuario; `devHintOrNull()` añade una pista técnica para Logcat.
 * - **Transitorios**: `consumeError()` / `consumeMessage()` permiten que la UI “limpie” los
 *   valores tras mostrarlos y evite re-mostrados en recomposición.
 *
 * ### Consideraciones
 * - **Verificación de email**: el login con email bloquea usuarios no verificados
 *   (se hace signOut y se informa), mientras que el registro ofrece `requestEmailVerificationAndLogout()`.
 * - **await() de Task**: se implementa con `suspendCancellableCoroutine`. La cancelación de la coroutine
 *   **no cancela** el `Task` de Firebase (documentado más abajo).
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {

    private companion object {
        const val TAG = "AuthViewModel"
    }

    /* ─────────── Estado observable UI (compatible) ─────────── */


    /** Usuario actual (null si no hay sesión; puede ser anónimo). */
    private val _user = MutableStateFlow(auth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    /** Indica si hay una operación de autenticación en curso. */
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /** Texto de error presentable (consumible por la UI). */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** Mensaje informativo presentable (consumible por la UI). */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** Eventos efímeros para snackbars/navegación (opcional en UI). */
    private val _events = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    /* ─────────── AuthStateListener ─────────── */

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        _user.value = firebaseAuth.currentUser
    }

    init {
        auth.addAuthStateListener(authListener)
        // Opcional: idioma por defecto para emails de verificación / reset
        auth.setLanguageCode("es")
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
        super.onCleared()
    }

    /* ─────────── Acciones públicas ─────────── */

    /** Inicia sesión anónima solo si no hay usuario. */
    fun signInAnonymouslyIfNeeded() {
        if (_user.value != null) return
        launchWithLoading {
            auth.signInAnonymously().await().user.also { _user.value = it }
            // No emitimos mensaje de éxito aquí para no molestar.
        }
    }
    /**
     * Inicio de sesión con email/contraseña.
     *
     * Reglas:
     * - Si el usuario no está verificado: se hace `signOut()` y se informa.
     * - En éxito: se actualiza [_user] (el listener también lo haría, pero lo reflejamos de inmediato).
     */
    fun signInWithEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            emitError("Introduce email y contraseña.")
            return
        }
        launchWithLoading {
            val user = auth.signInWithEmailAndPassword(email, password).await().user
            // ⚠️ Bloquea si no está verificado
            if (user != null && !user.isEmailVerified) {
                auth.signOut()
                emitError("Tu email aún no está verificado. Revisa tu correo y vuelve a intentarlo.")
                return@launchWithLoading
            }
            _user.value = user
            // Sin mensaje extra para no duplicar snackbars
        }
    }

    /**
     * Registro con email/contraseña.
     *
     * Comportamiento:
     * - Si el usuario actual es anónimo → **link** con las credenciales para conservar UID/datos.
     * - En caso contrario → crea cuenta y autentica con ella.
     * - Si `name` no es nulo/vacío → actualiza `displayName`.
     */
    fun registerWithEmail(
        name: String?,
        email: String,
        password: String,
        confirmPassword: String
    ) {
        if (email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            emitError("Rellena todos los campos necesarios.")
            return
        }
        if (password != confirmPassword) {
            emitError("Las contraseñas no coinciden.")
            return
        }
        if (password.length < 6) {
            emitError("La contraseña debe tener al menos 6 caracteres.")
            return
        }

        launchWithLoading {
            val current = auth.currentUser
            val credential = EmailAuthProvider.getCredential(email, password)

            val userAfter: FirebaseUser? = if (current != null && current.isAnonymous) {
                current.linkWithCredential(credential).await().user
            } else {
                auth.createUserWithEmailAndPassword(email, password).await().user
            }

            if (!name.isNullOrBlank() && userAfter != null) {
                val req = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                userAfter.updateProfile(req).await()
            }

            _user.value = userAfter
            _events.tryEmit(AuthEvent.RegisterSuccess)
        }
    }

    /** Envía correo para restablecer contraseña. */
    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            emitError("Introduce tu email.")
            return
        }
        launchWithLoading {
            auth.sendPasswordResetEmail(email).await()
            emitMessage("Te hemos enviado un correo para restablecer la contraseña.")
        }
    }

    /** Cierra sesión actual. */
    fun signOut() {
        try {
            auth.signOut()
            _events.tryEmit(AuthEvent.Info("Has cerrado sesión."))

        } catch (t: Throwable) {
            emitError(t.toUserMessage())
            t.devHintOrNull()?.let { Log.w(TAG, it, t) }
        }
    }

// ─────────────────────────────────────────────────────────────────────────────
// Verificación por email
// ─────────────────────────────────────────────────────────────────────────────
    /**
     * Solicita envío de email de verificación y luego **desconecta** al usuario,
     * emitiendo un evento de navegación a login para cerrar el flujo de registro.
     *
     * - Gestiona específicamente `ERROR_TOO_MANY_REQUESTS` con un mensaje amable.
     * - En cualquier caso (éxito o error), se cierra sesión en `finally`.
     */
    fun requestEmailVerificationAndLogout() {
        launchWithLoading {
            val u = auth.currentUser
            if (u == null) {
                emitError("No hay usuario autenticado para verificar.")
                // Aun así, salimos a Login para cerrar el flujo de registro
                try { auth.signOut() } catch (_: Throwable) {}
                _events.tryEmit(AuthEvent.NavigateToLogin)
                return@launchWithLoading
            }

            try {
                try {
                    u.sendEmailVerification().await()
                    emitMessage("Te hemos enviado un correo de verificación.")
                } catch (t: Throwable) {
                    val msg = if (t is FirebaseAuthException && t.errorCode == "ERROR_TOO_MANY_REQUESTS")
                        "Has alcanzado el límite de envíos. Revisa si ya te llegó el correo o inténtalo más tarde."
                    else t.toUserMessage()
                    emitError(msg)
                }
            } finally {
                try { auth.signOut() } catch (_: Throwable) {}
                _events.tryEmit(AuthEvent.NavigateToLogin)
            }
        }
    }

    /* ─────────── Helpers públicos para UI (evitar re-mostrado) ─────────── */

    /** Llama desde la UI tras mostrar el snackbar de error. */
    fun consumeError() { _error.value = null }

    /** Llama desde la UI tras mostrar el snackbar de mensaje. */
    fun consumeMessage() { _message.value = null }

    /* ─────────── Infra/privados ─────────── */

    private fun emitError(text: String) {
        _error.value = text
        _events.tryEmit(AuthEvent.Error(text))
    }

    private fun emitMessage(text: String) {
        _message.value = text
        _events.tryEmit(AuthEvent.Info(text))
    }
    /**
     * Ejecuta un bloque suspensivo mostrando `loading` y gestionando errores
     * con mapeo a mensajes de usuario + pista técnica en logs.
     */
    private fun launchWithLoading(block: suspend () -> Unit) {
        _loading.value = true
        // Al iniciar una acción cancelamos transitorios previos
        _error.value = null
        _message.value = null

        viewModelScope.launch {
            try {
                block()
            } catch (t: Throwable) {
                emitError(t.toUserMessage())
                t.devHintOrNull()?.let { Log.w(TAG, it, t) }
            } finally {
                _loading.value = false
            }
        }
    }

    /** Espera de forma suspensiva a que un [Task] termine. */
    private suspend fun <T> Task<T>.await(): T =
        suspendCancellableCoroutine { cont ->
            addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val result = task.result
                    if (cont.isActive) cont.resume(result)
                } else {
                    val ex = task.exception ?: RuntimeException("Error desconocido en Firebase Task")
                    if (cont.isActive) cont.resumeWithException(ex)
                }
            }
            // Nota: la cancelación de la coroutine no cancela el Task de Firebase.
        }

    // Mensaje presentable a usuario
    private fun Throwable.toUserMessage(): String = when (this) {
        is FirebaseNetworkException ->
            "Parece que no hay conexión. Inténtalo de nuevo."
        is FirebaseAuthException -> when (errorCode) {
            "ERROR_INVALID_EMAIL" -> "El email no tiene un formato válido."
            "ERROR_USER_NOT_FOUND", "ERROR_WRONG_PASSWORD" -> "Credenciales incorrectas. Revisa tu email o contraseña."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "Este email ya está en uso."
            "ERROR_WEAK_PASSWORD" -> "La contraseña es demasiado débil."
            "ERROR_OPERATION_NOT_ALLOWED" -> "No se pudo completar la operación. Inténtalo más tarde."
            "ERROR_APP_NOT_AUTHORIZED" -> "Se produjo un problema al iniciar sesión."
            "ERROR_NETWORK_REQUEST_FAILED" -> "No se pudo conectar. Revisa tu conexión."
            "ERROR_TOO_MANY_REQUESTS" -> "Demasiados intentos. Espera un momento."
            "ERROR_CREDENTIAL_ALREADY_IN_USE", "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
                "La cuenta ya existe con otras credenciales."
            else -> "No se pudo completar la operación."
        }
        else -> "Se produjo un error inesperado."
    }

    // Pista técnica para Logcat
    private fun Throwable.devHintOrNull(): String? = when (this) {
        is FirebaseNetworkException -> "Network error/timeout al contactar con Firebase."
        is FirebaseAuthException -> when (errorCode) {
            "ERROR_OPERATION_NOT_ALLOWED" -> "Proveedor deshabilitado en consola (Authentication → Sign-in method)."
            "ERROR_APP_NOT_AUTHORIZED" -> "App no autorizada: comprobar packageName/SHA y google-services.json."
            "ERROR_NETWORK_REQUEST_FAILED" -> "Fallo de red en Auth."
            "ERROR_TOO_MANY_REQUESTS" -> "Rate limiting de Firebase Auth."
            "ERROR_WEAK_PASSWORD" -> "Min 6 chars (o política superior)."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "Email ya en uso; considerar flujo de login."
            "ERROR_CREDENTIAL_ALREADY_IN_USE", "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
                "La identidad ya está vinculada; considerar recuperar e iniciar sesión."
            "ERROR_INVALID_EMAIL" -> "Formato de email inválido."
            "ERROR_USER_NOT_FOUND", "ERROR_WRONG_PASSWORD" -> "Credenciales incorrectas."
            else -> "FirebaseAuthException $errorCode: ${message.orEmpty()}"
        }
        else -> this.message
    }

    /** `true` si no hay usuario o es anónimo. Útil para UI (píldora de usuario). */
    @Suppress("unused")
    val isGuest: StateFlow<Boolean> =
        user.map { it == null || it.isAnonymous }
            .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    /** Etiqueta de usuario: “Invitado”, displayName o email. */
    @Suppress("unused")
    val userLabel: StateFlow<String> =
        user.map { u ->
            when {
                u == null || u.isAnonymous -> "Invitado"
                !u.displayName.isNullOrBlank() -> u.displayName!!
                !u.email.isNullOrBlank() -> u.email!!
                else -> "Usuario"
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, "Invitado")
}
