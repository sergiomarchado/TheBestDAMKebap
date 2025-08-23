package com.sergiom.thebestdamkebap.viewmodel.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sergiom.thebestdamkebap.data.address.Address
import com.sergiom.thebestdamkebap.data.address.AddressRepository
import com.sergiom.thebestdamkebap.data.profile.ProfileRepository
import com.sergiom.thebestdamkebap.domain.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * AddressListViewModel — **Mis direcciones**.
 *
 * Cambios clave (Clean/MVVM):
 * - Se inyecta [AuthRepository] (dominio) en lugar de depender de FirebaseAuth.
 * - El usuario actual se deriva de `authRepo.currentUser` (Flow) y, con `collectLatest`,
 *   se cancelan automáticamente las suscripciones a datos cuando cambia el usuario.
 * - `combine(addresses, profile)` produce la lista enriquecida con `isDefault`.
 *
 * UI:
 * - [UiState] inmutable hacia fuera y [Event] para snackbars one-shot.
 * - Misma semántica que la versión anterior (loading/guest/errores).
 */
@HiltViewModel
class AddressListViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val repo: AddressRepository,
    private val profiles: ProfileRepository
) : ViewModel() {

    /** Estado de UI para Compose (inmutable hacia fuera). */
    data class UiState(
        val loading: Boolean = true,
        val isGuest: Boolean = true,
        val addresses: List<Item> = emptyList(),
        val error: String? = null
    )

    /** Ítem de lista enriquecido con el flag `isDefault`. */
    data class Item(val address: Address, val isDefault: Boolean)

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    /** Eventos one-shot para snackbars. */
    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    sealed interface Event {
        data class Info(val text: String) : Event
        data class Error(val text: String) : Event
    }

    /** UID actual en memoria para acciones imperativas (setDefault/delete). */
    private var currentUid: String? = null

    init {
        // Cambios de usuario → cancelan las observaciones previas.
        viewModelScope.launch {
            authRepo.currentUser.collectLatest { du ->
                if (du == null || du.isAnonymous) {
                    currentUid = null
                    _ui.value = UiState(loading = false, isGuest = true)
                    return@collectLatest
                }

                currentUid = du.id
                _ui.update { it.copy(loading = true, isGuest = false, error = null) }

                combine(
                    repo.observeAddresses(du.id),
                    profiles.observeProfile(du.id)
                ) { list, prof ->
                    val defId = prof?.defaultAddressId
                    list.sortedBy { it.label ?: it.street }
                        .map { Item(address = it, isDefault = it.id == defId) }
                }
                    .catch {
                        _ui.update { it.copy(loading = false, error = "No se pudo cargar direcciones") }
                    }
                    .collect { items ->
                        _ui.update {
                            it.copy(
                                loading = false,
                                isGuest = false,
                                addresses = items,
                                error = null
                            )
                        }
                    }
            }
        }
    }

    /** Marca una dirección como **predeterminada**. */
    fun setDefault(aid: String) {
        val uid = currentUid ?: run {
            _events.tryEmit(Event.Error("Debes iniciar sesión"))
            return
        }
        viewModelScope.launch {
            runCatching { repo.setDefaultAddress(uid, aid) }
                .onSuccess { _events.tryEmit(Event.Info("Dirección predeterminada actualizada")) }
                .onFailure { t -> _events.tryEmit(Event.Error(t.message ?: "No se pudo establecer como predeterminada")) }
        }
    }

    /** Elimina una dirección del usuario. */
    fun delete(aid: String) {
        val uid = currentUid ?: run {
            _events.tryEmit(Event.Error("Debes iniciar sesión"))
            return
        }
        viewModelScope.launch {
            runCatching { repo.deleteAddress(uid, aid) }
                .onSuccess { _events.tryEmit(Event.Info("Dirección eliminada")) }
                .onFailure { t -> _events.tryEmit(Event.Error(t.message ?: "No se pudo eliminar")) }
        }
    }
}
