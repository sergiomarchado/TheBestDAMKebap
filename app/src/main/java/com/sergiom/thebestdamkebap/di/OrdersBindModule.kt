package com.sergiom.thebestdamkebap.di

import com.sergiom.thebestdamkebap.data.orders.FirebaseOrdersRepository
import com.sergiom.thebestdamkebap.domain.orders.OrdersRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OrdersBindModule {
    @Suppress("unused")
    @Binds @Singleton
    abstract fun bindOrdersRepository(impl: FirebaseOrdersRepository): OrdersRepository
}
