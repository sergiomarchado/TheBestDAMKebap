package com.sergiom.thebestdamkebap

import android.app.Application
import android.content.pm.ApplicationInfo
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.util.DebugLogger
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.sergiom.thebestdamkebap.core.imageloading.StorageUrlMemoryCache
import dagger.hilt.android.HiltAndroidApp

/**
 * Application raíz de la app.
 *
 * Responsabilidades:
 * - Inicializar Hilt (DI) mediante [HiltAndroidApp].
 * - Inicializar Firebase lo antes posible (idempotente).
 * - Configurar Firebase App Check:
 *   - En build *debug*: [DebugAppCheckProviderFactory] (permite ejecutar fuera de Play).
 *   - En build *release*: [PlayIntegrityAppCheckProviderFactory] (tokens válidos vía Play).
 *
 * Notas:
 * - La detección de build *debug* usa `ApplicationInfo.FLAG_DEBUGGABLE` para evitar
 *   depender de `BuildConfig`.
 * - No capturamos excepciones aquí: preferimos fallar ruidoso en desarrollo.
 * - Implementa [ImageLoaderFactory] para proporcionar el `ImageLoader` global de Coil v2.
 */
@HiltAndroidApp
class TheBestDamKebapApp : Application(), ImageLoaderFactory {

    // Evaluado on-demand; disponible tanto en onCreate() como en newImageLoader().
    private val isDebugBuild: Boolean by lazy {
        (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    override fun onCreate() {
        super.onCreate()

        // Inicializa Firebase (seguro aunque ya esté auto-inicializado por los servicios).
        FirebaseApp.initializeApp(this)

        // Selecciona el proveedor de App Check según el tipo de build.
        val factory = if (isDebugBuild) {
            DebugAppCheckProviderFactory.getInstance()
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }

        // Registra la factory en Firebase App Check: los SDKs adjuntarán tokens automáticamente.
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(factory)
    }

    // Coil v2: ImageLoader global para toda la app.
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            // Si tus URLs llevan versión/firmas (p. ej., de Firebase Storage), puedes ignorar headers.
            .respectCacheHeaders(false)
            .crossfade(true)
            .apply { if (isDebugBuild) logger(DebugLogger()) }
            .build()

    // Limpia la caché de URLs cuando la UI pasa a background.
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_UI_HIDDEN) {
            StorageUrlMemoryCache.clear()
        }
    }

    // Limpia la caché de URLs en memoria baja.
    override fun onLowMemory() {
        super.onLowMemory()
        StorageUrlMemoryCache.clear()
    }
}
