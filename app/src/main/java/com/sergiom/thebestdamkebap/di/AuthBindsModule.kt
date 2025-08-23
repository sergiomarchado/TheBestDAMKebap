package com.sergiom.thebestdamkebap.di

import com.sergiom.thebestdamkebap.domain.auth.AuthRepository
import com.sergiom.thebestdamkebap.data.auth.FirebaseAuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
/**
 * Módulo de **enlace (binding)** de Hilt para autenticación.
 *
 * Propósito:
 * - Conectar el **puerto de dominio** [AuthRepository] con su **implementación** concreta
 *   en la capa de datos [FirebaseAuthRepository].
 *
 * Alcance/vida:
 * - Se instala en [SingletonComponent], por lo que el binding vive toda la vida del proceso.
 * - El `@Singleton` en el método garantiza que la misma instancia de [FirebaseAuthRepository]
 *   se reutiliza cuando se inyecta [AuthRepository].
 *
 * Notas:
 * - Este módulo **no** crea `FirebaseAuth` ni nada más: eso lo hace `FirebaseModule`
 *   (para evitar duplicar proveedores).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthBindsModule {

    /**
     * Enlaza la implementación de datos [FirebaseAuthRepository] al contrato [AuthRepository].
     *
     * Requisitos de Hilt:
     * - Metodo `abstract` anotado con `@Binds`.
     * - La implementación debe tener `@Inject` en su constructor.
     * - El retorno es la **interfaz** que pedirá el resto de la app.
     */
    @Suppress("unused")
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: FirebaseAuthRepository
    ): AuthRepository
}
