package com.sergiom.thebestdamkebap.viewmodel.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sergiom.thebestdamkebap.domain.catalog.CatalogRepository
import com.sergiom.thebestdamkebap.domain.catalog.Category
import com.sergiom.thebestdamkebap.domain.catalog.Product
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
    session: OrderSessionRepository,
    private val savedState: SavedStateHandle
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val categories: List<Category> = emptyList(),
        val selectedCategoryId: String? = null,
        val products: List<Product> = emptyList(),
        val mode: OrderMode? = null,
        val browsingOnly: Boolean = false
    )

    private companion object {
        const val KEY_SEL_CAT = "selectedCategoryId"
    }

    // --- Fuentes base ---
    private val savedSelectedId: StateFlow<String?> =
        savedState.getStateFlow(KEY_SEL_CAT, null)

    private val sessionFlow: StateFlow<OrderContext> = session.context

    private val categoriesFlow: Flow<List<Category>> =
        catalog.observeCategories()
            .distinctUntilChanged()

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

    // Modelamos “cargando productos” por categoría sin vaciar la lista previa
    private sealed interface ProductsPhase {
        data object Loading : ProductsPhase
        data class Data(val items: List<Product>) : ProductsPhase
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val productsPhaseFlow: Flow<ProductsPhase> =
        selectedIdOrFirst.flatMapLatest { catId ->
            if (catId == null) {
                flowOf(ProductsPhase.Loading)
            } else {
                catalog.observeProducts(catId)
                    .distinctUntilChanged()
                    .map< List<Product>, ProductsPhase > { ProductsPhase.Data(it) }
                    .onStart { emit(ProductsPhase.Loading) } // ← loading al cambiar de categoría
            }
        }

    // --- UI STATE ---
    val ui: StateFlow<UiState> =
        combine(
            categoriesFlow,
            productsPhaseFlow,
            sessionFlow,
            selectedIdOrFirst
        ) { cats, phase, ctx, sel ->
            val isLoading = phase is ProductsPhase.Loading
            val items = (phase as? ProductsPhase.Data)?.items ?: emptyList()
            UiState(
                loading = isLoading,
                categories = cats,
                selectedCategoryId = sel,
                products = items,
                mode = ctx.mode,
                browsingOnly = ctx.browsingOnly
            )
        }
            // 👇 Mantén la lista anterior mientras `loading=true`
            .scan(UiState(loading = true)) { prev, next ->
                if (next.loading) next.copy(products = prev.products) else next
            }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                UiState(loading = true)
            )

    // --- Intents ---
    fun onSelectCategory(id: String) {
        if (id != savedSelectedId.value) {
            savedState[KEY_SEL_CAT] = id
        }
    }
}
