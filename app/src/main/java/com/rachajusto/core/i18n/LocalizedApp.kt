package com.rachajusto.core.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.PT_BR }

@Composable
fun ProvideAppLanguage(
    language: AppLanguage,
    languageManager: LanguageManager,
    content: @Composable () -> Unit
) {
    val baseContext = LocalContext.current
    val localizedContext = remember(baseContext, language) {
        languageManager.createLocalizedContext(baseContext, language)
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalAppLanguage provides language,
        content = content
    )
}
