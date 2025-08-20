package com.sergiom.thebestdamkebap.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ViewModel responsable del estado de autenticación.
 *
 * - UI: solo ve mensajes amigables.
 * - Dev: obtiene pistas técnicas en Logcat (TAG = "AuthViewModel").
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {

    private companion object {
        const val TAG = "AuthViewModel"
    }

    // --- Estado observable por la UI ---
    private val _user: MutableStateFlow<FirebaseUser?> = MutableStateFlow(auth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Mantener sincronizado con FirebaseAuth
    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        _user.value = firebaseAuth.currentUser
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
        super.onCleared()
    }

    /** Inicia sesión anónima solo si no hay usuario. */
    fun signInAnonymouslyIfNeeded() {
        if (_user.value != null) return

        _loading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val result = auth.signInAnonymously().await()
                _user.value = result.user // (el listener también lo ajustará)
            } catch (t: Throwable) {
                // 1) Mensaje para el usuario (breve y seguro)
                _error.value = t.toUserMessage()
                // 2) Pista técnica en Logcat (para desarrolladores)
                t.devHintOrNull()?.let { Log.w(TAG, it, t) }
            } finally {
                _loading.value = false
            }
        }
    }

    /** Cierra la sesión actual (si la hay). */
    fun signOut() {
        _error.value = null
        try {
            auth.signOut()
        } catch (t: Throwable) {
            _error.value = t.toUserMessage()
            t.devHintOrNull()?.let { Log.w(TAG, it, t) }
        }
    }

    // --- Helpers privados ---

    /** Espera de forma suspensiva a que un [Task] termine. */
    private suspend fun <T> Task<T>.await(): T =
        suspendCancellableCoroutine { cont ->
            addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    cont.resume(task.result)
                } else {
                    cont.resumeWithException(
                        task.exception ?: RuntimeException("Error desconocido en Firebase Task")
                    )
                }
            }
        }

    // Mensaje breve y presentable para la UI (sin detalles de consola)
    private fun Throwable.toUserMessage(): String = when (this) {
        is FirebaseNetworkException ->
            "Parece que no hay conexión. Inténtalo de nuevo."

        is FirebaseAuthException -> when (errorCode) {
            "ERROR_OPERATION_NOT_ALLOWED" ->
                "No se pudo iniciar sesión ahora. Inténtalo más tarde."
            "ERROR_APP_NOT_AUTHORIZED" ->
                "Se produjo un problema al iniciar sesión."
            "ERROR_NETWORK_REQUEST_FAILED" ->
                "No se pudo conectar. Revisa tu conexión."
            "ERROR_TOO_MANY_REQUESTS" ->
                "Demasiados intentos. Espera un momento."
            "ERROR_INTERNAL_ERROR" ->
                "Se produjo un error interno. Intenta de nuevo."
            else -> "No se pudo completar la operación. Inténtalo de nuevo."
        }

        else -> "Se produjo un error inesperado."
    }

    // Pista técnica SOLO para desarrolladores (va a Logcat, no a UI)
    private fun Throwable.devHintOrNull(): String? = when (this) {
        is FirebaseNetworkException ->
            "Network error/timeout al contactar con Firebase."

        is FirebaseAuthException -> when (errorCode) {
            "ERROR_OPERATION_NOT_ALLOWED" ->
                "Anonymous sign-in deshabilitado en la consola: Authentication → Sign-in method → Anonymous."
            "ERROR_APP_NOT_AUTHORIZED" ->
                "App no autorizada: comprueba packageName/SHA y el google-services.json del proyecto correcto."
            "ERROR_NETWORK_REQUEST_FAILED" ->
                "Fallo de red en Auth. Verifica conectividad/emulador proxy."
            "ERROR_TOO_MANY_REQUESTS" ->
                "Rate limiting de Firebase Auth."
            "ERROR_INTERNAL_ERROR" ->
                "Internal error de Auth (reintentar / revisar dependencias)."
            else -> "FirebaseAuthException con código $errorCode: ${message.orEmpty()}"
        }

        else -> this.message
    }
}
