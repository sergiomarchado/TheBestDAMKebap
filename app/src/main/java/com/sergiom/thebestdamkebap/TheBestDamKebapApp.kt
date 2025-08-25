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
 * - Inicializar Hilt (DI) mediante la anotación [HiltAndroidApp].
 * - Inicializar Firebase (idempotente) lo antes posible, para que los SDKs
 *   que se apoyan en él (Auth, Firestore, Analytics) estén listos.
 * - Configurar **Firebase App Check**:
 *   * En **debug** usa [DebugAppCheckProviderFactory] (permite ejecutar la app
 *     fuera de Google Play; si activas enforcement en consola, tendrás que registrar
 *     el *debug token* que se muestra en logs).
 *   * En **release** usa [PlayIntegrityAppCheckProviderFactory] (requiere distribución
 *     por Google Play para emitir tokens válidos).
 *
 * Decisiones:
 * - Detección de debug sin `BuildConfig`: leemos `ApplicationInfo.FLAG_DEBUGGABLE`.
 *   Esto evita depender de la generación de BuildConfig y funciona igual.
 * - No se capturan excepciones aquí: si App Check no puede instalarse, preferimos
 *   que falle visible en desarrollo para detectarlo pronto.
 *
 * Requisitos previos / comprobaciones rápidas:
 * - Manifest: declarar `android:name=".TheBestDamKebapApp"` en `<application>`.
 * - Google Services: tener `google-services.json` del proyecto correcto (appId/sha) y
 *   aplicar el plugin `com.google.gms.google-services`.
 * - Dependencias: usar BoM de Firebase y añadir los módulos de App Check (debug/playintegrity).
 * - Consola Firebase: habilitar **App Check** por servicio (Firestore/Storage/RTDB/Functions)
 *   y, en debug, registrar el **debug token** si activas enforcement.
 *
 * Compatibilidad/entorno:
 * - `PlayIntegrity` requiere dispositivos con Google Play Services y distribución por Play;
 *   en emuladores o tiendas sin Play no habrá token válido (con enforcement activo, se bloquearán
 *   las peticiones).
 * - App Check no “rompe” en sí mismo: el fallo se manifiesta cuando el backend con enforcement
 *   **rechaza** la solicitud; útil para probar rutas de error.
 */
@HiltAndroidApp
class TheBestDamKebapApp : Application(), ImageLoaderFactory {

    private var isDebugBuild: Boolean = false
    override fun onCreate() {
        super.onCreate()
        // Detectar "debuggeable"
        isDebugBuild = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        // Inicializa Firebase. Es seguro llamarlo aunque ya esté auto-inicializado:
        // FirebaseApp.initializeApp(...) es idempotente.
        FirebaseApp.initializeApp(this)


        // Selecciona el proveedor de App Check según el tipo de build:
        val factory = if (isDebugBuild) {
            DebugAppCheckProviderFactory.getInstance()
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }

        // Registra la factory en Firebase App Check. A partir de aquí, los SDKs de Firebase
        // pedirán y adjuntarán tokens de App Check automáticamente en sus peticiones.
        val appCheck = FirebaseAppCheck.getInstance()
        appCheck.installAppCheckProviderFactory(factory)

    }

    // Coil v2
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .respectCacheHeaders(false)
            .crossfade(true)
            .apply { if (isDebugBuild) logger(DebugLogger()) }
            .build()

    // Limpia tu caché de URLs cuando la UI se va al fondo
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_UI_HIDDEN) {
            StorageUrlMemoryCache.clear()
        }
    }

    // Limpia tu caché de URLs en memoria baja
    override fun onLowMemory() {
        super.onLowMemory()
        StorageUrlMemoryCache.clear()
    }
}