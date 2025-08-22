// viewmodel/profile/ProfileViewModel.kt
package com.sergiom.thebestdamkebap.viewmodel.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.sergiom.thebestdamkebap.data.profile.ProfileInput
import com.sergiom.thebestdamkebap.data.profile.ProfileRepository
import com.sergiom.thebestdamkebap.data.profile.UserProfile
import com.sergiom.thebestdamkebap.domain.profile.ValidateProfileInputUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val repo: ProfileRepository,
    private val validate: ValidateProfileInputUseCase // ⬅️ inyectamos el use case
) : ViewModel() {

    /** Estado del formulario + UI. */
    data class FormState(
        val givenName: String = "",
        val familyName: String = "",
        val phone: String = "",
        val birthDateMillis: Long? = null,
        val eGivenName: String? = null,
        val eFamilyName: String? = null,
        val ePhone: String? = null,
        val eBirthDate: String? = null,
        val canSave: Boolean = true
    )

    data class UiState(
        val loading: Boolean = false,
        val isGuest: Boolean = true,
        val email: String = "",
        val profile: UserProfile? = null,
        val form: FormState = FormState(),
        val saved: Boolean = false
    )

    private val _ui = MutableStateFlow(UiState(loading = true))
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    sealed interface Event {
        data class Info(val text: String) : Event
        data class Error(val text: String) : Event
    }

    private var ensuredOnce = false

    init {
        val u = auth.currentUser
        if (u == null || u.isAnonymous) {
            _ui.value = UiState(loading = false, isGuest = true)
        } else {
            val fallback = UserProfile(uid = u.uid, email = u.email, givenName = u.displayName)
            // Inicia form con fallback
            _ui.update {
                it.copy(
                    loading = true,
                    isGuest = false,
                    email = u.email.orEmpty(),
                    profile = fallback,
                    form = initialFormFrom(fallback)
                )
            }

            viewModelScope.launch {
                repo.observeProfile(u.uid)
                    .catch {
                        _events.tryEmit(Event.Error("No se pudo cargar el perfil"))
                        _ui.update { st -> st.copy(loading = false) }
                    }
                    .collect { prof ->
                        // Ensure si no existe doc
                        if (prof == null && !ensuredOnce) {
                            ensuredOnce = true
                            runCatching {
                                repo.upsertProfile(
                                    uid = u.uid,
                                    email = u.email,
                                    input = ProfileInput(
                                        givenName = u.displayName?.trim().takeUnless { it.isNullOrEmpty() }
                                    )
                                )
                            }
                        }
                        // Actualiza UI + form desde doc o fallback
                        val effective = prof ?: fallback
                        _ui.update {
                            it.copy(
                                loading = false,
                                profile = effective,
                                email = effective.email.orEmpty(),
                                form = it.form.copy(
                                    givenName = effective.givenName.orEmpty(),
                                    familyName = effective.familyName.orEmpty(),
                                    phone = effective.phone.orEmpty(),
                                    birthDateMillis = effective.birthDate?.time
                                ).revalidated()
                            )
                        }
                    }
            }
        }
    }

    /* ─────────── Intents de formulario ─────────── */
    fun onGivenNameChange(v: String) = updateForm { copy(givenName = v) }
    fun onFamilyNameChange(v: String) = updateForm { copy(familyName = v) }
    fun onPhoneChange(v: String)     = updateForm { copy(phone = v) }
    fun onBirthDateChange(millis: Long?) = updateForm { copy(birthDateMillis = millis) }

    /** Guarda cambios si el form es válido. */
    fun onSaveClicked() {
        val u = auth.currentUser
        if (u == null || u.isAnonymous) {
            _events.tryEmit(Event.Error("Debes iniciar sesión para editar tu perfil"))
            return
        }
        val current = _ui.value.form.revalidated()
        if (!current.canSave) {
            _events.tryEmit(Event.Error("Revisa los campos del formulario"))
            _ui.update { it.copy(form = current) } // para mostrar errores
            return
        }
        _ui.update { it.copy(loading = true, saved = false, form = current) }

        viewModelScope.launch {
            try {
                // Valida/sanea con el use case para generar el ProfileInput correcto
                val result = validate(
                    current.givenName,
                    current.familyName,
                    current.phone,
                    current.birthDateMillis
                )
                val input = ProfileInput(
                    givenName = result.sanitized.givenName,
                    familyName = result.sanitized.familyName,
                    phone = result.sanitized.phoneNormalized,
                    birthDateMillis = result.sanitized.birthDateMillis
                )
                val updated = repo.upsertProfile(u.uid, _ui.value.email, input)
                _ui.update { it.copy(loading = false, profile = updated, saved = true) }
                _events.tryEmit(Event.Info("Perfil guardado"))
            } catch (_: Throwable) {
                _ui.update { it.copy(loading = false, saved = false) }
                _events.tryEmit(Event.Error("No se pudo guardar el perfil"))
            }
        }
    }

    /** Restablece contraseña (Auth). */
    fun sendPasswordReset() {
        val email = auth.currentUser?.email
        if (email.isNullOrBlank()) {
            _events.tryEmit(Event.Error("Tu cuenta no tiene un email asociado."))
            return
        }
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                _events.tryEmit(Event.Info("Te hemos enviado un correo para restablecer la contraseña."))
            } catch (_: Throwable) {
                _events.tryEmit(Event.Error("No se pudo enviar el correo de restablecimiento."))
            }
        }
    }

    /* ─────────── Privados ─────────── */

    private fun initialFormFrom(p: UserProfile) = FormState(
        givenName = p.givenName.orEmpty(),
        familyName = p.familyName.orEmpty(),
        phone = p.phone.orEmpty(),
        birthDateMillis = p.birthDate?.time
    ).revalidated()

    private fun updateForm(transform: FormState.() -> FormState) {
        _ui.update { st ->
            val newForm = transform(st.form).revalidated()
            st.copy(form = newForm, saved = false)
        }
    }

    /** Revalida el form con el UseCase y rellena errores/canSave. */
    private fun FormState.revalidated(): FormState {
        val r = validate(givenName, familyName, phone, birthDateMillis)
        return copy(
            eGivenName = r.errors.givenName,
            eFamilyName = r.errors.familyName,
            ePhone = r.errors.phone,
            eBirthDate = r.errors.birthDate,
            canSave = r.valid
        )
    }
}

/* await() local para Task */
private suspend fun <T> Task<T>.await(): T =
    suspendCancellableCoroutine { cont ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (cont.isActive) cont.resume(task.result)
            } else {
                val ex = task.exception ?: RuntimeException("Error desconocido en Firebase Task")
                if (cont.isActive) cont.resumeWithException(ex)
            }
        }
    }
