package com.sergiom.thebestdamkebap.di

import com.sergiom.thebestdamkebap.data.profile.FirebaseProfileRepository
import com.sergiom.thebestdamkebap.domain.profile.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * ProfileModule
 *
 * Enlaza la interfaz [ProfileRepository] con su implementación real [FirebaseProfileRepository].
 *
 * Por qué así:
 * - Usamos `@Binds` para declarar una **única** implementación de la interfaz.
 * - Marcamos `@Singleton` porque el repositorio mantiene listeners/flows compartidos y
 *   no tiene sentido crear varias instancias en el mismo proceso.
 *
 * Ventajas:
 * - La app depende de la **interfaz** (fácil de testear: puedes inyectar un fake en tests).
 * - No hay acoplamiento a Firestore en capas superiores.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileModule {

    @Suppress("unused")
    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        impl: FirebaseProfileRepository
    ): ProfileRepository
}
