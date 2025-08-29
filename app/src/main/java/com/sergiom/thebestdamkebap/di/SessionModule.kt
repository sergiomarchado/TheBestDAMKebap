package com.sergiom.thebestdamkebap.di

import com.sergiom.thebestdamkebap.data.order.OrderSessionRepositoryImpl
import com.sergiom.thebestdamkebap.domain.order.OrderSessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SessionModule {
    @Suppress("unused")
    @Binds @Singleton
    abstract fun bindOrderSessionRepository(
        impl: OrderSessionRepositoryImpl
    ): OrderSessionRepository
}
