package com.sergiom.thebestdamkebap.core.localemanager

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocaleManager @Inject constructor() {
    /** Aplica inmediatamente el idioma. `null` => idioma del sistema. */
    fun apply(tagOrNull: String?) {
        val locales = if (tagOrNull.isNullOrBlank())
            LocaleListCompat.getEmptyLocaleList()
        else
            LocaleListCompat.forLanguageTags(tagOrNull)
        AppCompatDelegate.setApplicationLocales(locales)
    }
}