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

/**
 * ViewModel de la pantalla "Mi Perfil".
 *
 * Ideas clave:
 * - **No** conoce Firebase. Habla con "puertos" de dominio: [AuthRepository] y [ProfileRepository].
 * - Expone un **UiState** inmutable para Compose y **eventos** efímeros (snackbars).
 * - Valida la entrada con un **UseCase** de dominio antes de guardar.
 *
 * Ciclo de datos:
 * - Observa el usuario actual (authRepo.currentUser).
 * - Si hay usuario, observa su perfil (repo.observeProfile(uid)).
 * - Si el perfil aún no existe, hace un ensureProfile una sola vez.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepo: AuthRepository,          // Puerto de autenticación (dominio)
    private val repo: ProfileRepository,           // Puerto de perfil (dominio)
    private val validate: ValidateProfileInputUseCase // Caso de uso de validación (dominio)
) : ViewModel() {

    /**
     * Estado del **formulario**: valores que edita el usuario + errores.
     * - Strings vacíos por defecto para que los TextField no se quejen.
     * - `eCampo`: mensaje de error por campo (null = sin error).
     * - `canSave`: si el formulario pasa la validación.
     */
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

    /**
     * Estado **global** que consumirá la UI (Compose).
     * - `loading`: para mostrar progress y deshabilitar botones.
     * - `isGuest`: si no hay sesión o es anónima → no permitimos editar.
     * - `email`: el email visible (solo lectura) que viene de Auth.
     * - `profile`: el perfil de dominio (puede ser null si aún no existe).
     * - `form`: los valores que muestra/edita la UI.
     * - `saved`: para enseñar "Guardado" tras persistir.
     */
    data class UiState(
        val loading: Boolean = false,
        val isGuest: Boolean = true,
        val email: String = "",
        val profile: UserProfile? = null,
        val form: FormState = FormState(),
        val saved: Boolean = false
    )

    // StateFlow interno (mutable) + externo (solo lectura) para Compose
    private val _ui = MutableStateFlow(UiState(loading = true))
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    /**
     * **Eventos** de una sola vez (snackbars, info/errores).
     * Usamos SharedFlow en vez de LiveData para estar en coroutines puro.
     */
    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    sealed interface Event {
        data class Info(val text: String) : Event
        data class Error(val text: String) : Event
    }

    /**
     * Usuario actual como StateFlow para poder leerlo en cualquier momento.
     * - `stateIn` lo convierte en StateFlow y le da un valor inicial `null`.
     * - `SharingStarted.Eagerly`: empieza a colectar inmediatamente mientras viva el VM.
     */
    private val currentUser: StateFlow<DomainUser?> =
        authRepo.currentUser.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Para hacer ensureProfile una sola vez por UID (evitar bucles)
    private var ensureDoneForUid: String? = null

    init {
        // Arrancamos una corrutina que reacciona a cambios de usuario (login/logout)
        viewModelScope.launch {
            currentUser
                // Si cambia email o name pero NO el uid, no reiniciamos la pantalla
                .distinctUntilChanged { old, new -> old?.id == new?.id }
                // collectLatest: si cambia de usuario, cancela la suscripción previa al perfil
                .collectLatest { du ->
                    when {
                        // 1) Sin sesión o usuario anónimo → no editable
                        du == null || du.isAnonymous -> {
                            _ui.value = UiState(loading = false, isGuest = true)
                            ensureDoneForUid = null
                        }
                        else -> {
                            ensureDoneForUid = null // resetea la bandera al cambiar de usuario

                            // 2) Fallback rápido con datos de Auth por si Firestore tarda:
                            //    como el modelo de dominio tiene defaults, basta con uid/email/name.
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

                            // 3) Observa el documento de perfil. Si no existe → ensure al vuelo.
                            repo.observeProfile(du.id)
                                .catch {
                                    // Cualquier problema leyendo → avisamos y paramos el loading
                                    _events.tryEmit(Event.Error("No se pudo cargar el perfil"))
                                    _ui.update { st -> st.copy(loading = false) }
                                }
                                .collect { prof ->
                                    // Crea el doc de perfil una sola vez si no existe
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
                                        // No actualizamos aquí: esperamos al snapshot siguiente
                                    }

                                    // Efectivo = lo que venga de Firestore o el fallback
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

    /* ─────────── Intents del formulario (la UI llama a esto) ─────────── */

    fun onGivenNameChange(v: String)     = updateForm { copy(givenName = v) }
    fun onFamilyNameChange(v: String)    = updateForm { copy(familyName = v) }
    fun onPhoneChange(v: String)         = updateForm { copy(phone = v) }
    fun onBirthDateChange(millis: Long?) = updateForm { copy(birthDateMillis = millis) }

    /**
     * Guardar:
     * - Comprueba sesión.
     * - Vuelve a validar.
     * - Si OK, mapea a DomainProfileInput y pide al repo que haga un upsert (merge).
     */
    fun onSaveClicked() {
        val u = currentUser.value
        if (u == null || u.isAnonymous) {
            _events.tryEmit(Event.Error("Debes iniciar sesión para editar tu perfil"))
            return
        }

        // Revalida antes de guardar
        val current = _ui.value.form.revalidated()
        if (!current.canSave) {
            _events.tryEmit(Event.Error("Revisa los campos del formulario"))
            _ui.update { it.copy(form = current) }
            return
        }

        _ui.update { it.copy(loading = true, saved = false, form = current) }

        viewModelScope.launch {
            runCatching {
                // Valida/sanea con el caso de uso de dominio
                val result = validate(
                    current.givenName,
                    current.familyName,
                    current.phone,
                    current.birthDateMillis
                )
                // Solo enviamos al repo valores saneados (dominio)
                val input = DomainProfileInput(
                    givenName = result.sanitized.givenName,
                    familyName = result.sanitized.familyName,
                    phone = result.sanitized.phoneNormalized,
                    birthDateMillis = result.sanitized.birthDateMillis
                )

                // Upsert en el repositorio (la implementación ya hará el merge en data)
                val updated = repo.upsertProfile(
                    uid = u.id,
                    email = _ui.value.email.takeIf { it.isNotBlank() }, // si el email está vacío, no lo sobreescribe
                    input = input
                )

                // Feedback a la UI
                _ui.update { it.copy(loading = false, profile = updated, saved = true) }
                _events.tryEmit(Event.Info("Perfil guardado"))
            }.onFailure {
                _ui.update { it.copy(loading = false, saved = false) }
                _events.tryEmit(Event.Error("No se pudo guardar el perfil"))
            }
        }
    }

    /* ─────────── Privados: helpers internos ─────────── */

    // Construye el formulario inicial desde un perfil y lo deja ya validado
    private fun initialFormFrom(p: UserProfile) = FormState(
        givenName = p.givenName.orEmpty(),
        familyName = p.familyName.orEmpty(),
        phone = p.phone.orEmpty(),
        birthDateMillis = p.birthDateMillis
    ).revalidated()

    // Aplica cambios sobre el form actual, revalida y marca saved=false para feedback
    private fun updateForm(transform: FormState.() -> FormState) {
        _ui.update { st ->
            val newForm = transform(st.form).revalidated()
            st.copy(form = newForm, saved = false)
        }
    }

    // Llama al use case de validación y vuelca errores/canSave en el form
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

    /**
     * Envía un email de **restablecimiento de contraseña** usando el AuthRepository.
     * - Solo si el usuario tiene un email asociado.
     * - La UI mostrará un snackbar con el resultado (events).
     */
    fun sendPasswordReset() {
        val email = currentUser.value?.email
        if (email.isNullOrBlank()) {
            _events.tryEmit(Event.Error("Tu cuenta no tiene un email asociado."))
            return
        }
        viewModelScope.launch {
            try {
                authRepo.sendPasswordReset(email)
                _events.tryEmit(Event.Info("Te hemos enviado un correo para restablecer la contraseña."))
            } catch (_: Throwable) {
                _events.tryEmit(Event.Error("No se pudo enviar el correo de restablecimiento."))
            }
        }
    }
}
