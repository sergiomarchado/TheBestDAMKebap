package com.sergiom.thebestdamkebap.viewmodel.home.homestart

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sergiom.thebestdamkebap.domain.address.Address as DomainAddress
import com.sergiom.thebestdamkebap.domain.address.AddressRepository
import com.sergiom.thebestdamkebap.domain.auth.AuthRepository
import com.sergiom.thebestdamkebap.domain.auth.DomainUser
import com.sergiom.thebestdamkebap.domain.profile.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class HomeStartViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val addresses: AddressRepository,
    private val profiles: ProfileRepository,
    private val savedState: SavedStateHandle
) : ViewModel() {

    /** Modo de pedido elegido por el usuario. */
    enum class Mode { DELIVERY, PICKUP }

    /** Estado para la UI. */
    data class UiState(
        val loading: Boolean = true,
        val isGuest: Boolean = true,
        val mode: Mode = Mode.DELIVERY,
        val userEmail: String = "",
        val allAddresses: List<DomainAddress> = emptyList(),
        val selectedAddressId: String? = null, // solo aplica en DELIVERY
        val canStart: Boolean = false,         // según modo + selección
        val promos: List<Promo> = emptyList()
    )

    /** Datos mínimos para el carrusel de promos (Storage). */
    data class Promo(
        val id: String,
        val title: String,
        val storagePath: String
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    /** Eventos one-shot. */
    sealed interface Event {
        data class StartOrder(val mode: Mode, val addressId: String?) : Event
        data class Info(val text: String) : Event
        data class Error(val text: String) : Event
        data object GoToAddAddress : Event
        data object GoToAddressesList : Event
    }
    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    private val currentUser: StateFlow<DomainUser?> =
        auth.currentUser.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Preferencias rápidas (se restauran en recreaciones de proceso).
    private val modeFlow: StateFlow<Mode> =
        savedState.getStateFlow(KEY_MODE, Mode.DELIVERY)
    private val preferredIdFlow: StateFlow<String?> =
        savedState.getStateFlow(KEY_SELECTED_ID, null)

    init {
        // Observar usuario → (perfil + direcciones + preferencias)
        viewModelScope.launch {
            currentUser
                .distinctUntilChanged { a, b -> a?.id == b?.id }
                .collectLatest { du ->
                    if (du == null) {
                        _ui.value = UiState(loading = false, isGuest = true, promos = defaultPromos())
                        return@collectLatest
                    }

                    _ui.update {
                        it.copy(
                            loading = true,
                            isGuest = du.isAnonymous,
                            userEmail = du.email.orEmpty()
                        )
                    }

                    combine(
                        profiles.observeProfile(du.id),
                        addresses.observeAddresses(du.id),
                        modeFlow,
                        preferredIdFlow
                    ) { prof, list, mode, preferredId ->
                        val preferred = preferredId ?: prof?.defaultAddressId
                        val selected = chooseSelected(preferred, list, mode)

                        UiState(
                            loading = false,
                            isGuest = du.isAnonymous,
                            mode = mode,
                            userEmail = du.email.orEmpty(),
                            allAddresses = list,
                            selectedAddressId = selected,
                            canStart = canStart(mode, selected),
                            promos = defaultPromos()
                        )
                    }
                        .catch { _ ->
                            // Si algo falla (red/permisos), informamos y paramos el loading.
                            _events.tryEmit(Event.Error("No se pudo cargar tus datos."))
                            emit(_ui.value.copy(loading = false))
                        }
                        .collect { newState -> _ui.value = newState }
                }
        }
    }

    /* ─────────── Intents ─────────── */

    fun onModeChange(mode: Mode) {
        savedState[KEY_MODE] = mode
        _ui.update {
            val selected = chooseSelected(it.selectedAddressId, it.allAddresses, mode)
            it.copy(mode = mode, selectedAddressId = selected, canStart = canStart(mode, selected))
        }
    }

    fun onSelectAddress(addressId: String) {
        savedState[KEY_SELECTED_ID] = addressId
        _ui.update {
            it.copy(selectedAddressId = addressId, canStart = canStart(it.mode, addressId))
        }
    }

    fun onAddAddressClicked() {
        _events.tryEmit(Event.GoToAddAddress)
    }

    fun onManageAddressesClicked() {
        _events.tryEmit(Event.GoToAddressesList)
    }

    fun onStartOrderClicked() {
        val st = _ui.value
        if (!st.canStart) {
            _events.tryEmit(Event.Error("Selecciona una dirección para envío a domicilio"))
            return
        }
        _events.tryEmit(Event.StartOrder(st.mode, st.selectedAddressId))
    }

    /* ─────────── Privados ─────────── */

    private fun canStart(mode: Mode, addressId: String?): Boolean =
        when (mode) {
            Mode.PICKUP   -> true           // recoger no exige dirección
            Mode.DELIVERY -> addressId != null
        }

    /**
     * Si PICKUP → null.
     * Si DELIVERY → prioriza: [preferredId] (si existe) → primera de la lista → null.
     */
    private fun chooseSelected(
        preferredId: String?,
        list: List<DomainAddress>,
        mode: Mode
    ): String? {
        if (mode == Mode.PICKUP) return null
        val ids = list.asSequence().map { it.id }.toSet()
        return when {
            preferredId != null && preferredId in ids -> preferredId
            list.isNotEmpty() -> list.first().id
            else -> null
        }
    }

    /** Placeholder: cambia a Firestore/RemoteConfig cuando quieras hacerlo dinámico. */
    private fun defaultPromos(): List<Promo> = listOf(
        Promo("p1", "2x1 en durum de pollo", "promos/promo2x1durumpollo.webp"),
        Promo("p2", "Envío gratis > 20€",   "promos/promoenviogratis.webp"),
        Promo("p3", "Menú mediodía",         "promos/promo3.jpg"),
    )




    private companion object {
        const val KEY_MODE = "mode"
        const val KEY_SELECTED_ID = "selectedAddressId"
    }
}
