package com.sergiom.thebestdamkebap.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Guarda si el usuario quiere recordar el email y el último email usado.
 * (No guardamos contraseñas por seguridad).
 */
object UserPrefs {

    // DataStore singleton ligado al Context
    private val Context.dataStore by preferencesDataStore(name = "user_prefs")

    private val REMEMBER_EMAIL = booleanPreferencesKey("remember_email")
    private val SAVED_EMAIL = stringPreferencesKey("saved_email")

    /** Flujo del flag "recordar email". */
    fun rememberEmailFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[REMEMBER_EMAIL] ?: false }

    /** Flujo del email guardado ("" si no hay). */
    fun savedEmailFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[SAVED_EMAIL] ?: "" }

    /** Actualiza el flag "recordar email". */
    suspend fun setRememberEmail(context: Context, value: Boolean) {
        context.dataStore.edit { it[REMEMBER_EMAIL] = value }
    }

    /** Guarda el email (o lo elimina si va vacío). */
    suspend fun setSavedEmail(context: Context, email: String) {
        context.dataStore.edit { prefs ->
            if (email.isBlank()) prefs.remove(SAVED_EMAIL)
            else prefs[SAVED_EMAIL] = email
        }
    }

    /** Borra el email guardado. */
    suspend fun clearSavedEmail(context: Context) {
        context.dataStore.edit { it.remove(SAVED_EMAIL) }
    }
}
