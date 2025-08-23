// viewmodel/profile/ProfileViewModel.kt
package com.sergiom.thebestdamkebap.viewmodel.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sergiom.thebestdamkebap.data.profile.ProfileInput
import com.sergiom.thebestdamkebap.data.profile.ProfileRepository
import com.sergiom.thebestdamkebap.data.profile.UserProfile
import com.sergiom.thebestdamkebap.domain.auth.AuthRepository
import com.sergiom.thebestdamkebap.domain.auth.DomainUser
import com.sergiom.thebestdamkebap.domain.profile.ValidateProfileInputUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ProfileViewModel — pantalla **Mi Perfil**.
 *
 * Cambios clave (Clean/MVVM):
 * - Inyecta [AuthRepository] (dominio) en lugar de depender de FirebaseAuth.
 * - Deriva el usuario actual de `authRepo.currentUser` (reactivo).
 * - Observa `/users/{uid}` cancelando al cambiar de usuario (collectLatest).
 * - Delegación de `sendPasswordReset(email)` al repositorio de Auth.
 *
 * El resto del comportamiento de UI se mantiene: fallback inicial, ensure-once, validaciones y eventos.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val repo: ProfileRepository,
    private val validate: ValidateProfileInputUseCase
) : ViewModel() {

    /** Estado del formulario + errores de validación por campo. */
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

    /** Estado de UI consumido por Compose (inmutable hacia fuera). */
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

    /** Eventos efímeros de UI (snackbars). */
    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    sealed interface Event {
        data class Info(val text: String) : Event
        data class Error(val text: String) : Event
    }

    /** Usuario actual (dominio) como StateFlow para consultas puntuales (uid/email). */
    private val currentUser: StateFlow<DomainUser?> =
        authRepo.currentUser.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Bandera para no repetir `ensureProfile` por usuario. */
    private var ensureDoneForUid: String? = null

    init {
        // Cambios de usuario → cancelan la observación previa del perfil.
        viewModelScope.launch {
            currentUser
                // Evita re-acciones si cambian name/email pero NO el uid
                .distinctUntilChanged { old, new -> old?.id == new?.id }
                .collectLatest { du ->
                    when {
                        du == null || du.isAnonymous -> {
                            _ui.value = UiState(loading = false, isGuest = true)
                            ensureDoneForUid = null
                        }
                        else -> {
                            ensureDoneForUid = null // resetea el “ensure” al cambiar de usuario

                            // Fallback rápido con datos de Auth mientras llega Firestore
                            val fallback = UserProfile(
                                uid = du.id,
                                email = du.email,
                                givenName = du.name
                            )
                            _ui.update {
                                it.copy(
                                    loading = true,
                                    isGuest = false,
                                    email = du.email.orEmpty(),
                                    profile = fallback,
                                    form = initialFormFrom(fallback)
                                )
                            }

                            // Observa doc de perfil; si no existe, ensure 1 vez.
                            repo.observeProfile(du.id)
                                .catch {
                                    _events.tryEmit(Event.Error("No se pudo cargar el perfil"))
                                    _ui.update { st -> st.copy(loading = false) }
                                }
                                .collect { prof ->
                                    if (prof == null && ensureDoneForUid != du.id) {
                                        ensureDoneForUid = du.id
                                        runCatching {
                                            repo.ensureProfile(
                                                uid = du.id,
                                                email = du.email,
                                                seed = ProfileInput(
                                                    givenName = du.name?.trim()
                                                        .takeUnless { it.isNullOrEmpty() }
                                                )
                                            )
                                        }
                                        // El snapshot actualizará después.
                                    }

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
                                            ).revalidated(),
                                            saved = false
                                        )
                                    }
                                }
                        }
                    }
                }
        }
    }


    /* ─────────── Intents del formulario ─────────── */

    fun onGivenNameChange(v: String)        = updateForm { copy(givenName = v) }
    fun onFamilyNameChange(v: String)       = updateForm { copy(familyName = v) }
    fun onPhoneChange(v: String)            = updateForm { copy(phone = v) }
    fun onBirthDateChange(millis: Long?)    = updateForm { copy(birthDateMillis = millis) }

    /**
     * Guarda cambios si el formulario es válido:
     * - Revalida con el caso de uso de dominio.
     * - Upsert (merge) en `/users/{uid}`.
     */
    fun onSaveClicked() {
        val u = currentUser.value
        if (u == null || u.isAnonymous) {
            _events.tryEmit(Event.Error("Debes iniciar sesión para editar tu perfil"))
            return
        }

        val current = _ui.value.form.revalidated()
        if (!current.canSave) {
            _events.tryEmit(Event.Error("Revisa los campos del formulario"))
            _ui.update { it.copy(form = current) }
            return
        }

        _ui.update { it.copy(loading = true, saved = false, form = current) }

        viewModelScope.launch {
            try {
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

                val updated = repo.upsertProfile(
                    uid = u.id,
                    email = _ui.value.email.takeIf { it.isNotBlank() }, // ← este cambio
                    input = input
                )
                _ui.update { it.copy(loading = false, profile = updated, saved = true) }
                _events.tryEmit(Event.Info("Perfil guardado"))
            } catch (_: Throwable) {
                _ui.update { it.copy(loading = false, saved = false) }
                _events.tryEmit(Event.Error("No se pudo guardar el perfil"))
            }
        }
    }

    /** Envía correo de **restablecimiento de contraseña** (delegado a AuthRepository). */
    fun sendPasswordReset() {
        val email = currentUser.value?.email
        if (email.isNullOrBlank()) {
            _events.tryEmit(Event.Error("Tu cuenta no tiene un email asociado."))
            return
        }
        viewModelScope.launch {
            runCatching { authRepo.sendPasswordReset(email) }
                .onSuccess { _events.tryEmit(Event.Info("Te hemos enviado un correo para restablecer la contraseña.")) }
                .onFailure { _events.tryEmit(Event.Error("No se pudo enviar el correo de restablecimiento.")) }
        }
    }

    /* ─────────── Privados ─────────── */

    /** Construye el form inicial desde un perfil y lo deja validado. */
    private fun initialFormFrom(p: UserProfile) = FormState(
        givenName = p.givenName.orEmpty(),
        familyName = p.familyName.orEmpty(),
        phone = p.phone.orEmpty(),
        birthDateMillis = p.birthDate?.time
    ).revalidated()

    /** Aplica cambios al form, revalida y marca `saved=false` para feedback correcto. */
    private fun updateForm(transform: FormState.() -> FormState) {
        _ui.update { st ->
            val newForm = transform(st.form).revalidated()
            st.copy(form = newForm, saved = false)
        }
    }

    /** Ejecuta el caso de uso de validación y vuelca los errores/canSave al form. */
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
