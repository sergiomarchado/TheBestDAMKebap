package com.sergiom.thebestdamkebap.viewmodel.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.sergiom.thebestdamkebap.domain.profile.ProfileInput as DomainProfileInput
import com.sergiom.thebestdamkebap.domain.profile.ProfileRepository
import com.sergiom.thebestdamkebap.domain.auth.AuthRepository
import com.sergiom.thebestdamkebap.domain.auth.DomainUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel de Autenticación (MVVM + Clean).
 *
 * - Depende del puerto [AuthRepository] (no del SDK).
 * - Expone estado y eventos para Compose.
 * - Valida entrada básica y coordina con el repo de perfil.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepo: AuthRepository,     // Implementación real: FirebaseAuthRepository (capa data)
    private val profileRepo: ProfileRepository // Perfil en Firestore
) : ViewModel() {

    private companion object { const val TAG = "AuthViewModel" }

    /* ====================== Estado observable por la UI ====================== */

    /** Usuario actual (puede ser null o invitado). */
    val user: StateFlow<DomainUser?> =
        authRepo.currentUser.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Cargando (para deshabilitar y mostrar progress). */
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /** Errores y mensajes consumibles por la UI (snackbar/diálogo). */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)

    /** Eventos efímeros (navegación, avisos puntuales). */
    private val _events = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow() // ⬅️ expone solo lectura

    /* ====================== Acciones públicas ====================== */

    /** Crea sesión anónima si no hay usuario (el repo ya hace la comprobación). */
    fun signInAnonymouslyIfNeeded() = launchWithLoading {
        authRepo.signInAnonymouslyIfNeeded()
    }

    /**
     * Login con email/contraseña.
     * - Si el email no está verificado: cierra sesión y muestra aviso.
     * - Si todo OK: sincroniza email base en /users (best-effort).
     */
    fun signInWithEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank())
            return emitError("Introduce email y contraseña.")

        launchWithLoading {
            val u = authRepo.signInWithEmail(email, password)

            if (!u.isAnonymous && !u.isEmailVerified) {
                authRepo.signOut()
                emitError("Tu email aún no está verificado. Revisa tu correo y vuelve a intentarlo.")
                return@launchWithLoading
            }

            // Upsert no crítico: si falla, solo log
            runCatching {
                profileRepo.upsertProfile(
                    uid = u.id,
                    email = u.email,
                    input = DomainProfileInput()
                )
            }.onFailure { Log.w(TAG, "No se pudo sincronizar email en /users", it) }
        }
    }

    /**
     * Registro con email/contraseña (o link desde invitado).
     * - Si viene `name`, se usa como displayName (la capa data ya lo aplica).
     * - Upsert del perfil en Firestore (idempotente).
     */
    fun registerWithEmail(
        name: String?,
        email: String,
        password: String,
        confirmPassword: String
    ) {
        when {
            email.isBlank() || password.isBlank() || confirmPassword.isBlank() ->
                return emitError("Rellena todos los campos necesarios.")
            password != confirmPassword ->
                return emitError("Las contraseñas no coinciden.")
            password.length < 6 ->
                return emitError("La contraseña debe tener al menos 6 caracteres.")
        }

        launchWithLoading {
            val u: DomainUser = authRepo.registerWithEmail(name, email, password)

            runCatching {
                val input = DomainProfileInput(
                    givenName = name?.trim().takeUnless { it.isNullOrEmpty() }
                )
                profileRepo.upsertProfile(
                    uid = u.id,
                    email = u.email ?: email,
                    input = input
                )
            }.onFailure { Log.w(TAG, "No se pudo crear/actualizar el perfil en Firestore.", it) }

            _events.tryEmit(AuthEvent.RegisterSuccess)
        }
    }

    /** Envía correo de restablecimiento de contraseña. */
    fun sendPasswordReset(email: String) {
        if (email.isBlank()) return emitError("Introduce tu email.")
        launchWithLoading {
            authRepo.sendPasswordReset(email)
            emitMessage("Te hemos enviado un correo para restablecer la contraseña.")
        }
    }

    /** Cierra sesión actual. */
    fun signOut() {
        launchWithLoading {
            runCatching { authRepo.signOut() }
                .onSuccess { _events.tryEmit(AuthEvent.Info("Has cerrado sesión.")) }
                .onFailure { t ->
                    emitError(t.toUserMessage())
                    t.devHintOrNull()?.let { Log.w(TAG, it, t) }
                }
        }
    }

    /**
     * Envía email de verificación y, al terminar, cierra sesión → navega a Login.
     */
    fun requestEmailVerificationAndLogout() = launchWithLoading {
        try {
            runCatching { authRepo.requestEmailVerification() }
                .onSuccess { emitMessage("Te hemos enviado un correo de verificación.") }
                .onFailure { t ->
                    val msg =
                        if (t is FirebaseAuthException && t.errorCode == "ERROR_TOO_MANY_REQUESTS")
                            "Has alcanzado el límite de envíos. Revisa si ya te llegó el correo o inténtalo más tarde."
                        else t.toUserMessage()
                    emitError(msg)
                }
        } finally {
            runCatching { authRepo.signOut() }
            _events.tryEmit(AuthEvent.NavigateToLogin)
        }
    }

    /* ====================== Helpers UI (consumibles) ====================== */



    /* ====================== Infra / privados ====================== */

    private fun emitError(text: String) {
        _error.value = text
        _events.tryEmit(AuthEvent.Error(text))
    }

    private fun emitMessage(text: String) {
        _message.value = text
        _events.tryEmit(AuthEvent.Info(text))
    }

    /** Ejecuta un bloque mostrando `loading` y mapeando errores a mensajes de usuario. */
    private fun launchWithLoading(block: suspend () -> Unit) {
        _loading.value = true
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


    /* ====================== Mapeo de errores a mensajes ====================== */

    private fun Throwable.toUserMessage(): String = when (this) {
        is FirebaseNetworkException -> "Parece que no hay conexión. Inténtalo de nuevo."
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

    private fun Throwable.devHintOrNull(): String? = when (this) {
        is FirebaseNetworkException -> "Network error/timeout al contactar con Firebase."
        is FirebaseAuthException    -> "FirebaseAuthException ${errorCode}: ${message.orEmpty()}"
        else -> this.message
    }
}
