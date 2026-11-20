package com.rachajusto.core.i18n

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.PT_BR }
val LocalLocalizedContext = staticCompositionLocalOf<Context?> { null }

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
        LocalConfiguration provides localizedContext.resources.configuration,
        LocalLocalizedContext provides localizedContext,
        LocalAppLanguage provides language,
        content = content
    )
}

@Composable
fun WithAppLanguage(content: @Composable () -> Unit) {
    val localizedContext = LocalLocalizedContext.current
    if (localizedContext != null) {
        CompositionLocalProvider(
            LocalContext provides localizedContext,
            LocalConfiguration provides localizedContext.resources.configuration,
            content = content
        )
    } else {
        content()
    }
}
