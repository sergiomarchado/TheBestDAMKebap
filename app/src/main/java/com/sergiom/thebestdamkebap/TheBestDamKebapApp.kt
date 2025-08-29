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
import com.sergiom.thebestdamkebap.core.localemanager.LocaleManager
import com.sergiom.thebestdamkebap.domain.settings.AppSettingsRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * # TheBestDamKebapApp
 *
 * `Application` raíz. Se ejecuta **antes** que cualquier Activity y es el lugar para configurar
 * dependencias globales:
 *
 * - **Hilt**: `@HiltAndroidApp` genera los componentes de DI y enlaza el ciclo de vida de la app.
 * - **Firebase**: se inicializa tan pronto como sea posible (la llamada es idempotente).
 * - **Firebase App Check**: selecciona proveedor según el tipo de build:
 *   - **Debug** → [DebugAppCheckProviderFactory]: permite ejecutar fuera de Play (emulador, CI).
 *   - **Release** → [PlayIntegrityAppCheckProviderFactory]: emite tokens verificados por Play.
 * - **Coil v2**: implementa [ImageLoaderFactory] para proveer un `ImageLoader` global que la
 *   librería usará por defecto en toda la app.
 *
 * ### Notas
 * - `isDebugBuild` se calcula leyendo `ApplicationInfo.FLAG_DEBUGGABLE` para no depender de
 *   `BuildConfig` (útil en módulos compartidos).
 * - No capturamos excepciones aquí: fallar **temprano y ruidoso** ayuda durante desarrollo.
 * - Las URLs firmadas (p. ej. Firebase Storage) suelen ignorar headers de caché: ver `respectCacheHeaders(false)`.
 */
@HiltAndroidApp
class TheBestDamKebapApp : Application(), ImageLoaderFactory {

    // Inyectamos ajustes y aplicador de locales para el idioma.
    @Inject lateinit var settings: AppSettingsRepository
    @Inject
    lateinit var localeManager: LocaleManager

    /// Detecta si la app es "debuggable" (equivale a BuildConfig.DEBUG pero sin depender de él).
    private val isDebugBuild: Boolean by lazy {
        (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    override fun onCreate() {
        super.onCreate()

        // Inicializa Firebase. Seguro aunque ya haya sido auto-inicializado por Google Services.
        // Si google-services.json no está presente o es inválido, esta llamada devolverá null.
        FirebaseApp.initializeApp(this)

        // Selecciona el proveedor de App Check según el tipo de build:
        // - Debug: tokens "fake" para desarrollo/emulador.
        // - Release: Play Integrity (requiere Play Services/Play Store).
        val factory = if (isDebugBuild) {
            DebugAppCheckProviderFactory.getInstance()
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }

        // Registra la factory: los SDKs de Firebase adjuntarán tokens automáticamente en las peticiones.
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(factory)

        // Aplica el idioma guardado (null => seguir el sistema)
        val savedTag: String? = runBlocking { settings.languageTag.first() }
        localeManager.apply(savedTag)
    }

    // Provee el ImageLoader global que Coil utilizará por defecto.
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            // Para URLs firmadas que ya llevan versión/hash (ej. Firebase Storage), ignorar headers
            // evita "no-cache" innecesario y mejora hit-rate de caché.
            .respectCacheHeaders(false)
            // Transición de entrada agradable en cargas iniciales.
            .crossfade(true)
            // En builds debug, habilita logs detallados de Coil.
            .apply { if (isDebugBuild) logger(DebugLogger()) }
            .build()

    // Limpia la caché de URLs derivadas (tu caché en memoria) cuando la UI se oculta.
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_UI_HIDDEN) {
            StorageUrlMemoryCache.clear()
        }
    }

    // Limpia en memoria baja (evento más agresivo).
    override fun onLowMemory() {
        super.onLowMemory()
        StorageUrlMemoryCache.clear()
    }
}
