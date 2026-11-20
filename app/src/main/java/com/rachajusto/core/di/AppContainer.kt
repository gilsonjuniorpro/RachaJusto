package com.rachajusto.core.di

import android.content.Context
import com.rachajusto.core.i18n.LanguageManager

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val languageManager: LanguageManager by lazy {
        LanguageManager(appContext)
    }
}
