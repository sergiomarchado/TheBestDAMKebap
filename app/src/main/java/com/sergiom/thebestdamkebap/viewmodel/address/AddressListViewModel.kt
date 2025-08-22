// viewmodel/address/AddressListViewModel.kt
package com.sergiom.thebestdamkebap.viewmodel.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.sergiom.thebestdamkebap.data.address.Address
import com.sergiom.thebestdamkebap.data.address.AddressRepository
import com.sergiom.thebestdamkebap.data.profile.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddressListViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val repo: AddressRepository,
    private val profiles: ProfileRepository
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val isGuest: Boolean = true,
        val addresses: List<Item> = emptyList(),
        val error: String? = null
    )
    data class Item(val address: Address, val isDefault: Boolean)

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    sealed interface Event {
        data class Info(val text: String) : Event
        data class Error(val text: String) : Event
    }

    init {
        val u = auth.currentUser
        if (u == null || u.isAnonymous) {
            _ui.value = UiState(loading = false, isGuest = true)
        } else {
            viewModelScope.launch {
                combine(
                    repo.observeAddresses(u.uid),
                    profiles.observeProfile(u.uid)
                ) { list, prof ->
                    val defId = prof?.defaultAddressId
                    list.sortedBy { it.label ?: it.street }
                        .map { Item(address = it, isDefault = (it.id == defId)) }
                }.catch {
                    _ui.update { it.copy(loading = false, error = "No se pudo cargar direcciones") }
                }.collectLatest { items ->
                    _ui.update { it.copy(loading = false, isGuest = false, addresses = items, error = null) }
                }
            }
        }
    }

    fun setDefault(aid: String) {
        val u = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                repo.setDefaultAddress(u.uid, aid)
                _events.tryEmit(Event.Info("Dirección predeterminada actualizada"))
            } catch (t: Throwable) {
                // ← Mostrar el motivo real (permiso, validación, etc.)
                _events.tryEmit(Event.Error(t.message ?: "No se pudo establecer como predeterminada"))
            }
        }
    }

    fun delete(aid: String) {
        val u = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                repo.deleteAddress(u.uid, aid)
                _events.tryEmit(Event.Info("Dirección eliminada"))
            } catch (t: Throwable) {
                _events.tryEmit(Event.Error(t.message ?: "No se pudo eliminar"))
            }
        }
    }
}
