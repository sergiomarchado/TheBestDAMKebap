package com.sergiom.thebestdamkebap.di

import android.content.Context
import android.content.pm.ApplicationInfo
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de inyección de dependencias para los servicios de Firebase.
 *
 * Instalación/vida:
 * - Se instala en [SingletonComponent], por lo que cada @Provides aquí devuelve una
 *   **única instancia** (singleton) que vive durante todo el ciclo de vida del proceso.
 *
 * ¿Por qué inyectar en lugar de acceder estáticamente?
 * - Centraliza configuración (p. ej., `FirestoreSettings`) en un único punto.
 * - Hace el código **testeable** (en tests se pueden sustituir por dobles/fakes).
 * - Evita “new”/singletons manuales dispersos por la app.
 *
 * App Check:
 * - Este módulo **no** inicializa App Check; eso se hace en la clase `Application`.
 *   Si en la consola activas el “enforcement”, las llamadas de estas instancias
 *   ya viajarán con el token correspondiente.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    /**
     * Provee una instancia singleton de [FirebaseAuth].
     *
     * Razones:
     * - [FirebaseAuth.getInstance] es seguro y barato, pero inyectarlo facilita testear
     *   ViewModels/UseCases sin tocar APIs estáticas.
     */
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Provee una instancia singleton de [FirebaseFirestore] con ajustes de caché local.
     *
     * Diseño:
     * - La **persistencia offline** de Firestore en Android viene **activada por defecto**.
     *   Aquí solo la hacemos explícita y fijamos el tamaño de la caché.
     * - Elegimos un tamaño distinto según el tipo de build:
     *     * Debug: 20 MB → ahorro de espacio y feedback rápido durante desarrollo.
     *     * Release: 100 MB → más holgura para datos en producción.
     *
     * Por qué detectar “debug” así:
     * - Evitamos depender de BuildConfig.DEBUG. Leemos el flag `FLAG_DEBUGGABLE`, que
     *   funciona incluso si no generas BuildConfig.
     */
    @Provides
    @Singleton
    fun provideFirebaseFirestore(@ApplicationContext context: Context): FirebaseFirestore{
        val db = FirebaseFirestore.getInstance()

        // Detecta si la app es "debuggable" sin usar BuildConfig
        val isDebug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        // Tamaño de caché local según entorno
        val cacheBytes = if (isDebug){
            20L * 1024L * 1024L // 20MB en Debug
        } else{
            100L * 1024L * 1024L // 100MB en release
        }

        // FirestoreSettings moderno: usa PersistentCacheSettings para el tamaño de caché.
        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                com.google.firebase.firestore.PersistentCacheSettings
                    .newBuilder()
                    .setSizeBytes(cacheBytes)
                    .build()
            )
            .build()
        // Aplica los ajustes a la instancia antes de devolverla
        db.firestoreSettings = settings
        return db
    }

    /**
     * Provee una instancia singleton de [FirebaseAnalytics].
     *
     * Notas:
     * - Se obtiene con ApplicationContext para evitar fugas.
     * - Inyectarlo facilita testear (p. ej., sustituir por un stub que no envíe eventos).
     * - Si no quieres telemetry en Debug, puedes condicionar el envío en la capa que lo usa.
     */
    @Provides
    @Singleton
    fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalytics =
        FirebaseAnalytics.getInstance(context)
}