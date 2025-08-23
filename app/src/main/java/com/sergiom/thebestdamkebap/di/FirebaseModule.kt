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
 * Proveedor central de servicios de Firebase para la app.
 *
 * ¿Para qué sirve?
 * - Ofrece una única instancia de cada servicio que podemos pedir por inyección.
 * - Deja toda la configuración en un solo lugar, para que sea más fácil de mantener y probar.
 *
 * Alcance:
 * - Se instala a nivel de aplicación. Cada servicio se crea una vez y se reutiliza.
 *
 * Nota sobre App Check:
 * - La activación de App Check se realiza en la clase `Application`. Aquí solo
 *   entregamos las instancias ya listas para usarse.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    /**
     * Autenticación de Firebase.
     *
     * Por qué inyectarlo:
     * - Evita accesos estáticos repartidos por el código.
     * - Facilita sustituirlo por dobles o fakes en pruebas.
     */
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Base de datos Firestore con caché local ajustada por entorno.
     *
     * Comportamiento:
     * - Firestore ya trae caché offline activada; aquí solo fijamos el tamaño.
     * - En debug usamos un tamaño menor para ahorrar espacio; en release, más margen.
     */
    @Provides
    @Singleton
    fun provideFirebaseFirestore(@ApplicationContext context: Context): FirebaseFirestore{
        val db = FirebaseFirestore.getInstance()

        // Detectamos si la app es depurable sin depender de BuildConfig
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
     * Analytics de Firebase.
     *
     * Detalles:
     * - Se obtiene con el contexto de la aplicación.
     * - Si no quieres enviar eventos en debug, decide el comportamiento en la capa que lo use.
     */
    @Provides
    @Singleton
    fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalytics =
        FirebaseAnalytics.getInstance(context)
}