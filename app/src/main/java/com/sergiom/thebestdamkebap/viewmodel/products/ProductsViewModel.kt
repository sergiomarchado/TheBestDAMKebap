package com.sergiom.thebestdamkebap.viewmodel.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sergiom.thebestdamkebap.domain.catalog.CatalogRepository
import com.sergiom.thebestdamkebap.domain.catalog.Category
import com.sergiom.thebestdamkebap.domain.catalog.CategoryType
import com.sergiom.thebestdamkebap.domain.catalog.Product
import com.sergiom.thebestdamkebap.domain.menu.Menu
import com.sergiom.thebestdamkebap.domain.menu.MenuRepository
import com.sergiom.thebestdamkebap.domain.order.OrderContext
import com.sergiom.thebestdamkebap.domain.order.OrderMode
import com.sergiom.thebestdamkebap.domain.order.OrderSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val catalog: CatalogRepository,
    private val menus: MenuRepository,                 // ⬅️ NUEVO
    session: OrderSessionRepository,
    private val savedState: SavedStateHandle
) : ViewModel() {

    // Ítems unificados que la UI puede pintar (producto o menú)
    sealed interface CatalogItem {
        val id: String
        val name: String
        val description: String?
        val imagePath: String?
        val kind: Kind
        enum class Kind { PRODUCT, MENU }

        data class ProductItem(val product: Product) : CatalogItem {
            override val id = product.id
            override val name = product.name
            override val description = product.description
            override val imagePath = product.imagePath
            override val kind = Kind.PRODUCT
        }

        data class MenuItem(val menu: Menu) : CatalogItem {
            override val id = menu.id
            override val name = menu.name
            override val description = menu.description
            override val imagePath = menu.imagePath
            override val kind = Kind.MENU
        }
    }

    data class UiState(
        val loading: Boolean = true,
        val categories: List<Category> = emptyList(),
        val selectedCategoryId: String? = null,
        val items: List<CatalogItem> = emptyList(),      // ⬅️ unificado
        val mode: OrderMode? = null,
        val browsingOnly: Boolean = false
    )

    private companion object {
        const val KEY_SEL_CAT = "selectedCategoryId"
    }

    // Fuentes base
    private val savedSelectedId: StateFlow<String?> =
        savedState.getStateFlow(KEY_SEL_CAT, null)

    private val sessionFlow: StateFlow<OrderContext> = session.context

    private val categoriesFlow: Flow<List<Category>> =
        catalog.observeCategories().distinctUntilChanged()

    /**
     * Categoría efectiva: guardada o, si no hay, la primera.
     * Persistimos en SavedStateHandle solo la primera vez que aparece una id no nula.
     */
    private val selectedIdOrFirst: StateFlow<String?> =
        combine(savedSelectedId, categoriesFlow) { saved, cats ->
            saved ?: cats.firstOrNull()?.id
        }
            .distinctUntilChanged()
            .onEach { sel ->
                if (savedSelectedId.value == null && sel != null) {
                    savedState[KEY_SEL_CAT] = sel
                }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Encontrar la categoría seleccionada (objeto completo)
    private val selectedCategoryFlow: Flow<Category?> =
        combine(categoriesFlow, selectedIdOrFirst) { cats, sel ->
            cats.firstOrNull { it.id == sel }
        }.distinctUntilChanged()

    // Modela “cargando ítems” por categoría sin vaciar la lista previa
    private sealed interface ItemsPhase {
        data object Loading : ItemsPhase
        data class Data(val items: List<CatalogItem>) : ItemsPhase
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val itemsPhaseFlow: Flow<ItemsPhase> =
        selectedCategoryFlow.flatMapLatest { cat ->
            if (cat == null) {
                flowOf(ItemsPhase.Loading)
            } else {
                when (cat.type) {
                    CategoryType.MENUS -> {
                        menus.observeMenus()
                            .map { list ->
                                list
                                    .filter { it.active }
                                    .sortedBy { it.order }
                                    .map { CatalogItem.MenuItem(it) }
                            }
                    }
                    CategoryType.PRODUCTS -> {
                        catalog.observeProducts(cat.id)
                            .map { list -> list.map { CatalogItem.ProductItem(it) } }
                    }
                }
                    .distinctUntilChanged()
                    .map<List<CatalogItem>, ItemsPhase> { ItemsPhase.Data(it) }
                    .onStart { emit(ItemsPhase.Loading) } // loading al cambiar categoría
            }
        }

    // UI STATE
    val ui: StateFlow<UiState> =
        combine(
            categoriesFlow,
            itemsPhaseFlow,
            sessionFlow,
            selectedIdOrFirst
        ) { cats, phase, ctx, sel ->
            val isLoading = phase is ItemsPhase.Loading
            val items = (phase as? ItemsPhase.Data)?.items ?: emptyList()
            UiState(
                loading = isLoading,
                categories = cats,
                selectedCategoryId = sel,
                items = items,
                mode = ctx.mode,
                browsingOnly = ctx.browsingOnly
            )
        }
            // Mantén la lista anterior mientras loading = true
            .scan(UiState(loading = true)) { prev, next ->
                if (next.loading) next.copy(items = prev.items) else next
            }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                UiState(loading = true)
            )

    // Intents
    fun onSelectCategory(id: String) {
        if (id != savedSelectedId.value) {
            savedState[KEY_SEL_CAT] = id
        }
    }
    suspend fun loadProductsByIds(ids: List<String>): List<Product> =
        catalog.getProductsByIds(ids)
}
