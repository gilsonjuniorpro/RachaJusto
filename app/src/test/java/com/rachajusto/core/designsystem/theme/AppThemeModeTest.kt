package com.rachajusto.core.designsystem.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class AppThemeModeTest {

    @Test
    fun fromKey_resolvesModesAndDefaultsToSystem() {
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.fromKey("LIGHT"))
        assertEquals(AppThemeMode.DARK, AppThemeMode.fromKey("dark"))
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.fromKey("SYSTEM"))
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.fromKey(null))
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.fromKey("unknown"))
    }
}
