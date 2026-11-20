package com.rachajusto.core.i18n

import android.content.Context
import android.content.res.Configuration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class AppLanguage(
    val tag: String,
    val shortCode: String,
    val nativeName: String,
    val locale: Locale
) {
    PT_BR(
        tag = "pt-BR",
        shortCode = "PT",
        nativeName = "Português (Brasil)",
        locale = Locale.forLanguageTag("pt-BR")
    ),
    EN(
        tag = "en",
        shortCode = "EN",
        nativeName = "English",
        locale = Locale.ENGLISH
    ),
    ES(
        tag = "es",
        shortCode = "ES",
        nativeName = "Español",
        locale = Locale.forLanguageTag("es")
    ),
    FR(
        tag = "fr",
        shortCode = "FR",
        nativeName = "Français",
        locale = Locale.FRENCH
    );

    companion object {
        fun fromTag(tag: String?): AppLanguage {
            return entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) } ?: PT_BR
        }
    }
}

class LanguageManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentLanguage = MutableStateFlow(
        AppLanguage.fromTag(prefs.getString(KEY_LANGUAGE, AppLanguage.PT_BR.tag))
    )
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    fun setLanguage(language: AppLanguage) {
        if (_currentLanguage.value == language) return
        prefs.edit().putString(KEY_LANGUAGE, language.tag).apply()
        _currentLanguage.value = language
    }

    fun createLocalizedContext(baseContext: Context, language: AppLanguage): Context {
        val configuration = Configuration(baseContext.resources.configuration)
        configuration.setLocale(language.locale)
        return baseContext.createConfigurationContext(configuration)
    }

    private companion object {
        const val PREFS_NAME = "rachajusto_preferences"
        const val KEY_LANGUAGE = "app_language"
    }
}
