package com.sergiom.thebestdamkebap.viewmodel.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sergiom.thebestdamkebap.domain.address.Address as DomainAddress
import com.sergiom.thebestdamkebap.domain.address.AddressRepository
import com.sergiom.thebestdamkebap.domain.profile.ProfileRepository
import com.sergiom.thebestdamkebap.domain.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * AddressListViewModel — Mis direcciones (usa tipos de dominio).
 */
@HiltViewModel
class AddressListViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val repo: AddressRepository,
    private val profiles: ProfileRepository
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val isGuest: Boolean = true,
        val addresses: List<Item> = emptyList(),
        val error: String? = null
    )

    /** Ítem de lista con el flag de predeterminada. */
    data class Item(val address: DomainAddress, val isDefault: Boolean)

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    sealed interface Event {
        data class Info(val text: String) : Event
        data class Error(val text: String) : Event
    }

    /** UID actual para acciones imperativas (setDefault/delete). */
    private var currentUid: String? = null

    init {
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
                    repo.observeAddresses(du.id),      // Flow<List<DomainAddress>>
                    profiles.observeProfile(du.id)     // Flow<UserProfile?>
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

    /** Marca una dirección como predeterminada. */
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

    /** Elimina una dirección. */
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
