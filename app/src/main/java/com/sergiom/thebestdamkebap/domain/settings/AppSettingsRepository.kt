// domain/settings/AppSettingsRepository.kt
package com.sergiom.thebestdamkebap.domain.settings

import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {
    /** Tag BCP-47 (p.ej. "es", "en"); null => “Sistema”. */
    val languageTag: Flow<String?>
    suspend fun setLanguage(tag: String?)
}
