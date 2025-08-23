// di/AddressModule.kt
package com.sergiom.thebestdamkebap.di

import com.sergiom.thebestdamkebap.domain.address.AddressRepository
import com.sergiom.thebestdamkebap.data.address.FirebaseAddressRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AddressModule {
    @Binds @Singleton
    abstract fun bindAddressRepository(impl: FirebaseAddressRepository): AddressRepository
}
