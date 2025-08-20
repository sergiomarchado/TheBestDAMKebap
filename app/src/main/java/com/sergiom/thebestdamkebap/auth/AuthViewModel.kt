package com.sergiom.thebestdamkebap.auth

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
 * Soporta:
 * - Inicio anónimo (invitado).
 * - Inicio con email/contraseña.
 * - Registro (crea cuenta o "mejora" usuario anónimo mediante link).
 * - Envío de email para restablecer contraseña.
 *
 * UI/Dev:
 * - La UI consume estados: [user], [loading], [error] y [message].
 * - Mensajes para usuario son genéricos; pistas técnicas van a Logcat.
 */

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {

    private companion object {
        const val TAG = "AuthViewModel"
    }

    //---- Estado observable UI ----

    // Usuario actual o null si no ha iniciado sesión
    private val _user =
        MutableStateFlow(auth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    // Indicar si hay operación en uso
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    // Último error visto
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Mensajes informativos de éxito
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()


    // Sincronización del StateFlow con el estado real de FirebaseAuth
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

    // ------------------------------------------------------------
    // Invitado (anónimo)
    // ------------------------------------------------------------
    /** Inicia sesión anónima solo si no hay usuario. */
    fun signInAnonymouslyIfNeeded() {
        if (_user.value != null) return
        launchWithLoading {
            auth.signInAnonymously().await().user.also { _user.value = it }
            _message.value = null // no mostramos "éxito" aquí para no molestar
        }
    }

    // Helper de signInAnonymouslyIfNeeded
    private fun launchWithLoading(block: suspend () -> Unit) {
        _error.value = null
        _message.value = null
        _loading.value = true
        viewModelScope.launch {
            try {
                block()
            } catch (t: Throwable) {
                _error.value = t.toUserMessage()
                t.devHintOrNull()?.let { Log.w(TAG, it, t) }
            } finally {
                _loading.value = false
            }
        }
    }

    // HELPER
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

    // Mensaje breve y presentable para la UI (sin detalles técnicos)
    private fun Throwable.toUserMessage(): String = when (this) {
        is FirebaseNetworkException ->
            "Parece que no hay conexión. Inténtalo de nuevo."

        is FirebaseAuthException -> when (errorCode) {
            "ERROR_INVALID_EMAIL" ->
                "El email no tiene un formato válido."
            "ERROR_USER_NOT_FOUND", "ERROR_WRONG_PASSWORD" ->
                "Credenciales incorrectas. Revisa tu email o contraseña."
            "ERROR_EMAIL_ALREADY_IN_USE" ->
                "Este email ya está en uso."
            "ERROR_WEAK_PASSWORD" ->
                "La contraseña es demasiado débil."
            "ERROR_OPERATION_NOT_ALLOWED" ->
                "No se pudo completar la operación. Inténtalo más tarde."
            "ERROR_APP_NOT_AUTHORIZED" ->
                "Se produjo un problema al iniciar sesión."
            "ERROR_NETWORK_REQUEST_FAILED" ->
                "No se pudo conectar. Revisa tu conexión."
            "ERROR_TOO_MANY_REQUESTS" ->
                "Demasiados intentos. Espera un momento."
            "ERROR_CREDENTIAL_ALREADY_IN_USE", "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
                "La cuenta ya existe con otras credenciales."
            else -> "No se pudo completar la operación."
        }

        else -> "Se produjo un error inesperado."
    }

    // Pista técnica SOLO para desarrolladores (Logcat)
    private fun Throwable.devHintOrNull(): String? = when (this) {
        is FirebaseNetworkException ->
            "Network error/timeout al contactar con Firebase."
        is FirebaseAuthException -> when (errorCode) {
            "ERROR_OPERATION_NOT_ALLOWED" ->
                "Proveedor deshabilitado en consola (ver Authentication → Sign-in method)."
            "ERROR_APP_NOT_AUTHORIZED" ->
                "App no autorizada: comprobar packageName/SHA y google-services.json."
            "ERROR_NETWORK_REQUEST_FAILED" ->
                "Fallo de red en Auth."
            "ERROR_TOO_MANY_REQUESTS" ->
                "Rate limiting de Firebase Auth."
            "ERROR_WEAK_PASSWORD" ->
                "Min 6 chars (o política superior)."
            "ERROR_EMAIL_ALREADY_IN_USE" ->
                "Email ya en uso; considerar flujo de login."
            "ERROR_CREDENTIAL_ALREADY_IN_USE", "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
                "La identidad ya está vinculada; considerar recuperar e iniciar sesión."
            "ERROR_INVALID_EMAIL" ->
                "Formato de email inválido."
            "ERROR_USER_NOT_FOUND", "ERROR_WRONG_PASSWORD" ->
                "Credenciales incorrectas."
            else -> "FirebaseAuthException $errorCode: ${message.orEmpty()}"
        }
        else -> this.message
    }


    // ------------------------------------------------------------
    // Login con email/contraseña
    // ------------------------------------------------------------

    fun signInWithEmail(email: String, password: String){
        //Validación rápida en VM (también podemos validar en UI)
        if(email.isBlank() || password.isBlank()){
            _error.value = "Introduce email y contraseña."
            return
        }
        launchWithLoading {
            auth.signInWithEmailAndPassword(email, password).await().user.also {
                _user.value = it
            }
            _message.value = null
        }
    }

    // ------------------------------------------------------------
    // Registro (link si el usuario actual es anónimo; crear cuenta si no)
    // ------------------------------------------------------------

    fun registerWithEmail(
        name: String?,
        email: String,
        password: String,
        confirmPassword: String
    ){
        // Validación mínima
        if(email.isBlank() || password.isBlank() || confirmPassword.isBlank()){
            _error.value = "Rellena todos los campos necesarios."
            return
        }
        if(password!= confirmPassword){
            _error.value = "Las contraseñas no coinciden."
            return
        }
        if(password.length < 6){
            _error.value = "La contraseña debe tener al menos 6 caracteres."
            return
        }

        launchWithLoading {
            val current = auth.currentUser
            val credential = EmailAuthProvider.getCredential(email,password)

            val userAfter: FirebaseUser? = if(current != null && current.isAnonymous){
                // Caso 1: Mejora de cuenta invitado -> email y contraseña
                current.linkWithCredential(credential).await().user
            } else{
                // Crear cuenta nueva (si había sesión previa, Firebase cambiará al nuevo usuario)
                auth.createUserWithEmailAndPassword(email,password).await().user
            }

            // Actualiza el displayName si se ha proporcionado.
            if(!name.isNullOrBlank() && userAfter != null){
                val req = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                userAfter.updateProfile(req).await()
            }

            _user.value = userAfter
            _message.value = "Tu cuenta se ha creado correctamente."
        }
    }

    // ------------------------------------------------------------
    // Reset de contraseña
    // ------------------------------------------------------------

    fun sendPasswordReset(email:String){
        if(email.isBlank()){
            _error.value = "Introduce tu email."
            return
        }
        launchWithLoading {
            auth.sendPasswordResetEmail(email).await()
            _message.value = "Te hemos enviado un correo para restablecer la constraseña."
        }
    }

    // ------------------------------------------------------------
    // Cerrar sesión
    // ------------------------------------------------------------

    fun signOut(){
        _error.value = null
        _message.value = null

        try {
            auth.signOut()
        }catch (t: Throwable){
            _error.value = t.toUserMessage()
            t.devHintOrNull()?.let { Log.w(TAG, it, t) }
        }
    }

}
