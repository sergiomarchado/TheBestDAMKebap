package com.sergiom.thebestdamkebap.di

import com.sergiom.thebestdamkebap.data.menu.FirebaseMenuRepository
import com.sergiom.thebestdamkebap.domain.menu.MenuRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
abstract class MenuBindModule {
    @Binds
    @Singleton
    abstract fun bindMenuRepository(
        impl: FirebaseMenuRepository
    ): MenuRepository
}
