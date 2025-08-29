// di/SettingsModule.kt
@file:Suppress("unused")

package com.sergiom.thebestdamkebap.di

import com.sergiom.thebestdamkebap.domain.settings.AppSettingsRepository
import com.sergiom.thebestdamkebap.data.settings.AppSettingsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {
    @Binds
    @Singleton
    abstract fun bindAppSettingsRepository(
        impl: AppSettingsRepositoryImpl
    ): AppSettingsRepository
}
