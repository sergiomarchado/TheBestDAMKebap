// di/AddressDomainModule.kt
@file:Suppress("unused")
package com.sergiom.thebestdamkebap.di

import com.sergiom.thebestdamkebap.domain.address.ValidateAddressInputUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AddressDomainModule {
    @Provides @Singleton
    fun provideValidateAddressInputUseCase() = ValidateAddressInputUseCase()
}