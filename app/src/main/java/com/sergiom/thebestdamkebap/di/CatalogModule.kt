// di/CatalogModules.kt
package com.sergiom.thebestdamkebap.di

import com.sergiom.thebestdamkebap.data.catalog.FirebaseCatalogRepository
import com.sergiom.thebestdamkebap.domain.catalog.CatalogRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CatalogBindModule {
    @Suppress("unused")
    @Binds
    @Singleton
    abstract fun bindCatalogRepository(
        impl: FirebaseCatalogRepository
    ): CatalogRepository
}
