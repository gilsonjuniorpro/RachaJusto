package com.rachajusto.core.designsystem.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromKey(key: String?): AppThemeMode {
            return entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: SYSTEM
        }
    }
}

class ThemePreferenceManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        AppThemeMode.fromKey(prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name))
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        if (_themeMode.value == mode) return
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    private companion object {
        const val PREFS_NAME = "rachajusto_preferences"
        const val KEY_THEME_MODE = "app_theme_mode"
    }
}
