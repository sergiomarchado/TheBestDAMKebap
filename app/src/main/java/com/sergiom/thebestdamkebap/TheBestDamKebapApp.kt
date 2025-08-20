package com.sergiom.thebestdamkebap

import android.app.Application
import android.content.pm.ApplicationInfo
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
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
 */
@HiltAndroidApp
class TheBestDamKebapApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Inicializa Firebase. Es seguro llamarlo aunque ya esté auto-inicializado:
        // FirebaseApp.initializeApp(...) es idempotente.
        FirebaseApp.initializeApp(this)

        // Detectar "debuggeable"
        val isDebug = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        // Selecciona el proveedor de App Check según el tipo de build:
        val factory = if(isDebug){
            DebugAppCheckProviderFactory.getInstance()
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }

        // Registra la factory en Firebase App Check. A partir de aquí, los SDKs de Firebase
        // pedirán y adjuntarán tokens de App Check automáticamente en sus peticiones.
        val appCheck = FirebaseAppCheck.getInstance()
        appCheck.installAppCheckProviderFactory(factory)

    }
}