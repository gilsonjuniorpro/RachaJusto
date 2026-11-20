package com.rachajusto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rachajusto.core.designsystem.theme.RachaJustoTheme
import com.rachajusto.core.i18n.ProvideAppLanguage
import com.rachajusto.navigation.RachaJustoNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = (application as RachaJustoApplication).appContainer
        setContent {
            val currentLanguage by appContainer.languageManager.currentLanguage.collectAsStateWithLifecycle()
            ProvideAppLanguage(
                language = currentLanguage,
                languageManager = appContainer.languageManager
            ) {
                RachaJustoTheme {
                    RachaJustoNavHost(appContainer = appContainer)
                }
            }
        }
    }
}
