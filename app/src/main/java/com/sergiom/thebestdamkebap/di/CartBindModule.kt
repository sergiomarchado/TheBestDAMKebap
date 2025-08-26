package com.sergiom.thebestdamkebap.di

import com.sergiom.thebestdamkebap.data.cart.InMemoryCartRepository
import com.sergiom.thebestdamkebap.domain.cart.CartRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CartBindModule {
    @Binds @Singleton
    abstract fun bindCartRepository(impl: InMemoryCartRepository): CartRepository
}
