package com.sergiom.thebestdamkebap.view.home.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergiom.thebestdamkebap.core.firebase.rememberStorage
import com.sergiom.thebestdamkebap.view.home.screens.components.products.CategoryRow
import com.sergiom.thebestdamkebap.view.home.screens.components.products.ProductList
import com.sergiom.thebestdamkebap.viewmodel.products.ProductsViewModel

@Composable
fun ProductsScreen(
    viewModel: ProductsViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    val storage = rememberStorage()

    Column(Modifier.fillMaxSize()) {
        CategoryRow(
            categories = ui.categories,
            selectedId = ui.selectedCategoryId,
            onSelect = viewModel::onSelectCategory
        )
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        when {
            ui.loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            ui.products.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay productos en esta categoría")
                }
            }
            else -> {
                ProductList(
                    products = ui.products,
                    mode = ui.mode,
                    storage = storage
                )
            }
        }
    }
}







