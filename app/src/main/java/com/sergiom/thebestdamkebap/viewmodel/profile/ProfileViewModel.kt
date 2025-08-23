// viewmodel/profile/ProfileViewModel.kt
package com.sergiom.thebestdamkebap.viewmodel.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sergiom.thebestdamkebap.domain.profile.ProfileRepository
import com.sergiom.thebestdamkebap.domain.profile.ProfileInput as DomainProfileInput
import com.sergiom.thebestdamkebap.domain.auth.AuthRepository
import com.sergiom.thebestdamkebap.domain.auth.DomainUser
import com.sergiom.thebestdamkebap.domain.profile.UserProfile
import com.sergiom.thebestdamkebap.domain.profile.ValidateProfileInputUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val repo: ProfileRepository,
    private val validate: ValidateProfileInputUseCase
) : ViewModel() {

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

    private val currentUser: StateFlow<DomainUser?> =
        authRepo.currentUser.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var ensureDoneForUid: String? = null

    init {
        viewModelScope.launch {
            currentUser
                .distinctUntilChanged { old, new -> old?.id == new?.id }
                .collectLatest { du ->
                    when {
                        du == null || du.isAnonymous -> {
                            _ui.value = UiState(loading = false, isGuest = true)
                            ensureDoneForUid = null
                        }
                        else -> {
                            ensureDoneForUid = null

                            // ⚠️ DOMINIO: hay que pasar todos los campos requeridos
                            val fallback = UserProfile(
                                uid = du.id,
                                email = du.email,
                                givenName = du.name,
                                familyName = null,
                                phone = null,
                                birthDateMillis = null,
                                defaultAddressId = null,
                                createdAtMillis = null,
                                updatedAtMillis = null
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
                                                seed = DomainProfileInput(
                                                    givenName = du.name?.trim()
                                                        .takeUnless { it.isNullOrEmpty() }
                                                )
                                            )
                                        }
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
                                                // ⚠️ DOMINIO: birthDateMillis (no birthDate)
                                                birthDateMillis = effective.birthDateMillis
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

    fun onGivenNameChange(v: String)     = updateForm { copy(givenName = v) }
    fun onFamilyNameChange(v: String)    = updateForm { copy(familyName = v) }
    fun onPhoneChange(v: String)         = updateForm { copy(phone = v) }
    fun onBirthDateChange(millis: Long?) = updateForm { copy(birthDateMillis = millis) }

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
            runCatching {
                val result = validate(
                    current.givenName,
                    current.familyName,
                    current.phone,
                    current.birthDateMillis
                )
                // ⚠️ DOMINIO: usar DomainProfileInput (no el de data)
                val input = DomainProfileInput(
                    givenName = result.sanitized.givenName,
                    familyName = result.sanitized.familyName,
                    phone = result.sanitized.phoneNormalized,
                    birthDateMillis = result.sanitized.birthDateMillis
                )

                val updated = repo.upsertProfile(
                    uid = u.id,
                    email = _ui.value.email.takeIf { it.isNotBlank() },
                    input = input
                )
                _ui.update { it.copy(loading = false, profile = updated, saved = true) }
                _events.tryEmit(Event.Info("Perfil guardado"))
            }.onFailure {
                _ui.update { it.copy(loading = false, saved = false) }
                _events.tryEmit(Event.Error("No se pudo guardar el perfil"))
            }
        }
    }

    /* ─────────── Privados ─────────── */

    private fun initialFormFrom(p: UserProfile) = FormState(
        givenName = p.givenName.orEmpty(),
        familyName = p.familyName.orEmpty(),
        phone = p.phone.orEmpty(),
        // ⚠️ DOMINIO: usar birthDateMillis
        birthDateMillis = p.birthDateMillis
    ).revalidated()

    private fun updateForm(transform: FormState.() -> FormState) {
        _ui.update { st ->
            val newForm = transform(st.form).revalidated()
            st.copy(form = newForm, saved = false)
        }
    }

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
}
