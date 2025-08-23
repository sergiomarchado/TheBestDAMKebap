// di/ProfileDomainModule.kt
@file:Suppress("unused")

package com.sergiom.thebestdamkebap.di

import com.sergiom.thebestdamkebap.domain.profile.ValidateProfileInputUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * ProfileDomainModule
 *
 * Expone el caso de uso de **validación de perfil** como dependencia inyectable.
 *
 * Notas:
 * - Es un caso de uso **sin estado** y barato de crear; lo registramos como `@Singleton`
 *   para tener una sola instancia en tod el proceso (también podrías usar `@Reusable`).
 * - Usamos `@Provides` porque es una clase concreta (no interfaz) y no necesitamos `@Binds`.
 */
@Module
@InstallIn(SingletonComponent::class)
object ProfileDomainModule {

    @Provides
    @Singleton
    fun provideValidateProfileInputUseCase(): ValidateProfileInputUseCase =
        ValidateProfileInputUseCase()
}
