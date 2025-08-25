package com.sergiom.thebestdamkebap.domain.catalog

import kotlinx.coroutines.flow.Flow

interface CatalogRepository {
    /** Categorías activas ordenadas por `order`. */
    fun observeCategories(): Flow<List<Category>>

    /** Productos activos. Si [categoryId] != null, filtrados por esa categoría. */
    fun observeProducts(categoryId: String?): Flow<List<Product>>

    //Lectura puntual por ids
    suspend fun getProductsByIds(ids: List<String>): List<Product>
}
