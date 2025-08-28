package com.sergiom.thebestdamkebap.viewmodel.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.FirebaseFirestore
import com.sergiom.thebestdamkebap.domain.auth.AuthRepository
import com.sergiom.thebestdamkebap.domain.cart.CartRepository
import com.sergiom.thebestdamkebap.domain.order.OrderSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@HiltViewModel
class SettingsViewModel @Inject constructor(
    authRepo: AuthRepository,
    private val cart: CartRepository,
    private val session: OrderSessionRepository,
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    data class UiState(
        val loading: Boolean = false,
        val isGuest: Boolean = true,
        val email: String? = null,
        val error: String? = null,
        val success: Boolean = false,
        val needsReauth: Boolean = false
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    sealed interface Event {
        data class Info(val msg: String) : Event
        data class Error(val msg: String) : Event
    }
    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    val events = _events

    init {
        // Refleja el estado de sesión (email si la hay)
        viewModelScope.launch {
            authRepo.currentUser
                .map { du -> UiState(isGuest = (du == null), email = du?.email) }
                .stateIn(viewModelScope, SharingStarted.Eagerly, UiState())
                .collect { base -> _ui.update { base.copy(loading = it.loading, error = it.error, success = it.success, needsReauth = it.needsReauth) } }
        }
    }

    /** Elimina TODOS los datos del usuario (perfil + direcciones) y luego su cuenta de Auth. */
    fun deleteAccount(confirmPassword: String?) {
        viewModelScope.launch {
            if (_ui.value.loading) return@launch
            val user = auth.currentUser
            if (user == null) {
                _ui.update { it.copy(error = "No has iniciado sesión") }
                return@launch
            }

            _ui.update { it.copy(loading = true, error = null, success = false, needsReauth = false) }

            try {
                // Si el proveedor es email/contraseña y tenemos contraseña, reautentica
                if (!confirmPassword.isNullOrBlank() && !user.email.isNullOrBlank()) {
                    val cred = EmailAuthProvider.getCredential(user.email!!, confirmPassword)
                    user.reauthenticate(cred).await()
                }

                // 1) Borrar datos Firestore del usuario (antes de borrar Auth)
                purgeUserData(user.uid)

                // 2) Borrar cuenta de autenticación
                user.delete().await()

                // 3) Limpiar estado local (carrito + sesión)
                cart.clear()
                session.clear()

                _ui.update { it.copy(loading = false, success = true) }
                _events.tryEmit(Event.Info("Tu cuenta se ha eliminado correctamente."))

            } catch (e: Exception) {
                val needsReauth = e is FirebaseAuthRecentLoginRequiredException
                val msg = when {
                    needsReauth -> "Por seguridad, vuelve a iniciar sesión o introduce la contraseña para continuar."
                    e.message?.contains("INVALID_LOGIN_CREDENTIALS", true) == true -> "Contraseña incorrecta."
                    else -> e.message ?: "No se pudo eliminar la cuenta."
                }
                _ui.update { it.copy(loading = false, error = msg, needsReauth = needsReauth) }
                _events.tryEmit(Event.Error(msg))
            }
        }
    }

    /** Borra `/users/{uid}` y su subcolección `addresses`. (No borra pedidos por políticas/rules). */
    private suspend fun purgeUserData(uid: String) {
        val userRef = db.collection("users").document(uid)
        // 1) Borrar subcolección addresses (documento a documento)
        val addrSnap = userRef.collection("addresses").get().await()
        if (!addrSnap.isEmpty) {
            val batch = db.batch()
            addrSnap.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
        // 2) Borrar el documento de usuario
        userRef.delete().await()
        // NOTA: pedidos no se borran (rules no lo permiten); además es recomendable conservarlos.
    }
}
