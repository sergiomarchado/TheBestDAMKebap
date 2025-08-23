// viewmodel/address/AddressEditViewModel.kt
package com.sergiom.thebestdamkebap.viewmodel.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sergiom.thebestdamkebap.data.address.Address
import com.sergiom.thebestdamkebap.data.address.AddressInput
import com.sergiom.thebestdamkebap.data.address.AddressRepository
import com.sergiom.thebestdamkebap.domain.address.ValidateAddressInputUseCase
import com.sergiom.thebestdamkebap.domain.auth.AuthRepository
import com.sergiom.thebestdamkebap.domain.auth.DomainUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel de **Alta/Edición de Dirección**.
 *
 * Cambios clave (Clean/MVVM):
 * - Inyecta [AuthRepository] (dominio) en lugar de depender de FirebaseAuth.
 * - Lee el usuario actual desde `authRepo.currentUser` (StateFlow).
 * - Se mantiene el mismo flujo: bootstrap una sola vez, validación dominio, upsert y (opcional) setDefault.
 *
 * Notas:
 * - Este VM no reacciona si cambia el usuario *después* de `bootstrap` (mismo comportamiento que tenías).
 *   Si en el futuro quieres soportarlo, se podría observar `authRepo.currentUser` y reinicializar.
 */
@HiltViewModel
class AddressEditViewModel @Inject constructor(
    authRepo: AuthRepository,
    private val repo: AddressRepository,
    private val validate: ValidateAddressInputUseCase
) : ViewModel() {

    /** Estado del formulario + errores de validación. */
    data class FormState(
        val label: String = "",
        val recipientName: String = "",
        val phone: String = "",
        val street: String = "",
        val number: String = "",
        val floorDoor: String = "",
        val city: String = "",
        val province: String = "",
        val postalCode: String = "",
        val notes: String = "",
        // errores
        val eStreet: String? = null,
        val eNumber: String? = null,
        val eCity: String? = null,
        val ePostalCode: String? = null,
        val ePhone: String? = null,
        // flags
        val canSave: Boolean = false,
        val setAsDefault: Boolean = false
    )

    /** Estado de UI consumido por Compose. */
    data class UiState(
        val loading: Boolean = true,
        val isGuest: Boolean = true,
        val aid: String? = null,            // null = creación; no-null = edición
        val form: FormState = FormState(),
        val error: String? = null,
        val saved: Boolean = false          // true tras guardar correctamente
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    /** Usuario actual como StateFlow (para consultar uid/email cuando hace falta). */
    private val currentUser: StateFlow<DomainUser?> =
        authRepo.currentUser.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var currentUid: String? = null
    private var bootstrapped = false

    /**
     * Inicializa la pantalla una sola vez.
     * - Si `aid` es null → form vacío validado.
     * - Si `aid` tiene valor → observa la dirección y precarga datos.
     */
    fun bootstrap(aid: String?) {
        if (bootstrapped) return
        bootstrapped = true

        val du = currentUser.value
        if (du == null || du.isAnonymous) {
            _ui.value = UiState(loading = false, isGuest = true, aid = aid)
            return
        }
        currentUid = du.id

        if (aid.isNullOrBlank()) {
            _ui.value = UiState(
                loading = false,
                isGuest = false,
                aid = null,
                form = FormState().revalidated()
            )
        } else {
            viewModelScope.launch {
                repo.observeAddress(du.id, aid)
                    .catch {
                        _ui.update { it.copy(loading = false, error = "No se pudo cargar la dirección") }
                    }
                    .collect { addr ->
                        _ui.value = UiState(
                            loading = false,
                            isGuest = false,
                            aid = aid,
                            form = (addr?.toForm() ?: FormState()).revalidated()
                        )
                    }
            }
        }
    }

    /* ─────────── Intents del formulario ─────────── */

    /** Aplica cambios al form, revalida y limpia errores globales. */
    fun edit(transform: FormState.() -> FormState) {
        _ui.update { st -> st.copy(form = transform(st.form).revalidated(), saved = false, error = null) }
    }

    /** Marca/desmarca “establecer como predeterminada”. */
    fun toggleDefault(v: Boolean) = edit { copy(setAsDefault = v) }

    /**
     * Guarda si el formulario es válido.
     * - Normaliza y valida con el caso de uso de dominio.
     * - `upsertAddress()` (merge); si es nueva, devuelve el `id` creado.
     * - Si procede, marca como predeterminada.
     */
    fun save(onDone: (String) -> Unit) {
        val uid = currentUid ?: run {
            _ui.update { it.copy(error = "Debes iniciar sesión") }
            return
        }
        val st = _ui.value
        val f = st.form.revalidated()
        if (!f.canSave) {
            _ui.update { it.copy(form = f, error = "Revisa el formulario") }
            return
        }
        _ui.update { it.copy(loading = true, error = null, form = f) }

        viewModelScope.launch {
            try {
                val r = validate(
                    f.label, f.recipientName, f.phone, f.street, f.number, f.floorDoor,
                    f.city, f.province, f.postalCode, f.notes
                )
                val id = repo.upsertAddress(
                    uid = uid,
                    aid = st.aid,
                    input = AddressInput(
                        label = r.sanitized.label,
                        recipientName = r.sanitized.recipientName,
                        phone = r.sanitized.phoneNormalized,
                        street = r.sanitized.street,
                        number = r.sanitized.number,
                        floorDoor = r.sanitized.floorDoor,
                        city = r.sanitized.city,
                        province = r.sanitized.province,
                        postalCode = r.sanitized.postalCode,
                        notes = r.sanitized.notes
                    )
                )
                if (f.setAsDefault) repo.setDefaultAddress(uid, id)

                _ui.update { it.copy(loading = false, saved = true, aid = id) }
                onDone(id) // cierre delegado a la pantalla (un solo camino de salida)
            } catch (_: Throwable) {
                _ui.update { it.copy(loading = false, saved = false, error = "No se pudo guardar") }
            }
        }
    }

    /* ─────────── Privados ─────────── */

    private fun Address.toForm() = FormState(
        label = label.orEmpty(),
        recipientName = recipientName.orEmpty(),
        phone = phone.orEmpty(),
        street = street,
        number = number,
        floorDoor = floorDoor.orEmpty(),
        city = city,
        province = province.orEmpty(),
        postalCode = postalCode,
        notes = notes.orEmpty()
    )

    /** Ejecuta validación de dominio y rellena errores/canSave. */
    private fun FormState.revalidated(): FormState {
        val r = validate(label, recipientName, phone, street, number, floorDoor, city, province, postalCode, notes)
        return copy(
            eStreet = r.errors.street,
            eNumber = r.errors.number,
            eCity = r.errors.city,
            ePostalCode = r.errors.postalCode,
            ePhone = r.errors.phone,
            canSave = r.valid
        )
    }
}
