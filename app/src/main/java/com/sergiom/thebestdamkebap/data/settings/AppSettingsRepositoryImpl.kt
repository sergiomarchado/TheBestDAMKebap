// data/settings/AppSettingsRepositoryImpl.kt
package com.sergiom.thebestdamkebap.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sergiom.thebestdamkebap.domain.settings.AppSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private const val DS_NAME = "app_settings"
private val Context.appSettings: DataStore<Preferences> by preferencesDataStore(DS_NAME)

@Singleton
class AppSettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val app: Context
) : AppSettingsRepository {

    private object Keys { val LANG = stringPreferencesKey("language_tag") }

    override val languageTag: Flow<String?> =
        app.appSettings.data
            .catch { emit(emptyPreferences()) }
            .map { it[Keys.LANG] }

    override suspend fun setLanguage(tag: String?) {
        app.appSettings.edit { prefs ->
            if (tag.isNullOrBlank()) prefs.remove(Keys.LANG)
            else prefs[Keys.LANG] = tag
        }
    }
}
