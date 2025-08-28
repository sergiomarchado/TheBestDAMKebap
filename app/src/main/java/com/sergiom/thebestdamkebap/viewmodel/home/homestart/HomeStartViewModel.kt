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

/**
 * ViewModel de la portada de Home (HomeStartScreen).
 *
 * Responsabilidades:
 * - Observar usuario actual (invitado/registrado).
 * - Combinar perfil + direcciones + preferencias locales (modo / dirección elegida).
 * - Exponer [UiState] inmutable y eventos one-shot (navegación / avisos).
 * - Recordar selecciones con [SavedStateHandle] para sobrevivir recreaciones.
 *
 * Notas:
 * - `currentUser` se reduce a cambios de `uid` para evitar trabajo innecesario.
 * - Las promos se sirven como lista estática (no recalculada en cada emisión).
 */
@HiltViewModel
class HomeStartViewModel @Inject constructor(
    auth: AuthRepository,
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

    /** Eventos one-shot (navegación/avisos). */
    sealed interface Event {
        data class StartOrder(val mode: Mode, val addressId: String?) : Event
        data class Info(val text: String) : Event
        data class Error(val text: String) : Event
        object GoToAddAddress : Event
        object GoToAddressesList : Event
    }
    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    /** Usuario actual (null → sin sesión; isAnonymous → invitado). */
    private val currentUser: StateFlow<DomainUser?> =
        auth.currentUser.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // ───────── Preferencias ligeras (sobreviven a recreación de proceso) ─────────
    private val modeFlow: StateFlow<Mode> =
        savedState.getStateFlow(KEY_MODE, Mode.DELIVERY)

    private val preferredIdFlow: StateFlow<String?> =
        savedState.getStateFlow(KEY_SELECTED_ID, null)

    // ───────── Promos (estáticas para no recalcular en cada emisión) ─────────
    private val promosStatic: List<Promo> = listOf(
        Promo("p1", "Promo1", "promos/promo2x1durumpollo.webp"),
        Promo("p2", "Promo2", "promos/promoenviogratis.webp"),
        Promo("p3", "Promo3", "promos/promocombodoble.webp"),
    )

    init {
        // Observar usuario → combinar (perfil + direcciones + preferencias)
        viewModelScope.launch {
            currentUser
                // Reaccionar solo si cambia el uid (ignora cambios no relevantes)
                .distinctUntilChanged { a, b -> a?.id == b?.id }
                .collectLatest { du ->
                    if (du == null) {
                        // Sin sesión: invitado
                        _ui.value = UiState(
                            loading = false,
                            isGuest = true,
                            promos = promosStatic
                        )
                        return@collectLatest
                    }

                    // Hay usuario: mostrar loading hasta combinar fuentes
                    _ui.update {
                        it.copy(
                            loading = true,
                            isGuest = du.isAnonymous,
                            userEmail = du.email.orEmpty()
                        )
                    }

                    combine(
                        // Estos suelen ser Flow "normales" → sí tiene sentido distinctUntilChanged
                        profiles.observeProfile(du.id).distinctUntilChanged(),
                        addresses.observeAddresses(du.id).distinctUntilChanged(),
                        // Estos son StateFlow → NO usar distinctUntilChanged
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
                            promos = promosStatic
                        )
                    }
                        .catch {
                            // Error de red/permisos → notificar y salir de loading
                            _events.tryEmit(Event.Error("No se pudo cargar tus datos."))
                            emit(_ui.value.copy(loading = false))
                        }
                        .collect { newState -> _ui.value = newState }
                }
        }
    }

    // ─────────────────────────────── Intents de la UI ───────────────────────────────

    /** Cambiar el modo (DELIVERY / PICKUP). Se persiste en SavedStateHandle. */
    fun onModeChange(mode: Mode) {
        savedState[KEY_MODE] = mode
        _ui.update {
            val selected = chooseSelected(it.selectedAddressId, it.allAddresses, mode)
            it.copy(
                mode = mode,
                selectedAddressId = selected,
                canStart = canStart(mode, selected)
            )
        }
    }

    /** Seleccionar dirección para envío (sólo efecto en DELIVERY). */
    fun onSelectAddress(addressId: String) {
        savedState[KEY_SELECTED_ID] = addressId
        _ui.update {
            it.copy(
                selectedAddressId = addressId,
                canStart = canStart(it.mode, addressId)
            )
        }
    }

    /** CTA: abrir pantalla para añadir una dirección nueva. */
    fun onAddAddressClicked() {
        _events.tryEmit(Event.GoToAddAddress)
    }

    /** CTA: abrir gestión/listado de direcciones. */
    fun onManageAddressesClicked() {
        _events.tryEmit(Event.GoToAddressesList)
    }

    /** CTA principal: empezar pedido (valida según el modo). */
    fun onStartOrderClicked() {
        val st = _ui.value
        if (!st.canStart) {
            _events.tryEmit(Event.Error("Selecciona una dirección para envío a domicilio"))
            return
        }
        _events.tryEmit(Event.StartOrder(st.mode, st.selectedAddressId))
    }

    // ─────────────────────────────── Privados ───────────────────────────────

    /** Regla de habilitado del CTA según modo. */
    private fun canStart(mode: Mode, addressId: String?): Boolean =
        when (mode) {
            Mode.PICKUP   -> true           // recoger no exige dirección
            Mode.DELIVERY -> addressId != null
        }

    /**
     * Estrategia de selección de dirección:
     * - Si PICKUP → null (no se usa).
     * - Si DELIVERY → prioriza: [preferredId] (si sigue en la lista) → primera → null.
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

    private companion object {
        const val KEY_MODE = "mode"
        const val KEY_SELECTED_ID = "selectedAddressId"
    }
}
