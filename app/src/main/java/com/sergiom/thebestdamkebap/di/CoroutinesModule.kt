package com.sergiom.thebestdamkebap.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Marca para identificar el **scope de aplicación** al inyectar un [CoroutineScope].
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object CoroutinesModule {

    /**
     * Módulo de configuración de corrutinas.
     *
     * ¿Para qué sirve?
     * - Expone un [CoroutineScope] que vive mientras la aplicación esté en memoria.
     * - Útil para alojar `StateFlow` o tareas de larga duración que no dependan de
     *   una pantalla o ViewModel concreto.
     *
     * Detalles:
     * - Se instala como singleton, por lo que es un único scope en toda la app.
     * - Se crea con [SupervisorJob] (un fallo en una tarea no cancela las demás).
     * - Usa [Dispatchers.IO] porque está orientado a trabajo en segundo plano.
     */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
