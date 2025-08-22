package com.sergiom.thebestdamkebap.di

import com.sergiom.thebestdamkebap.data.profile.FirebaseProfileRepository
import com.sergiom.thebestdamkebap.data.profile.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileModule {
    @Suppress("unused")
    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: FirebaseProfileRepository): ProfileRepository
}
