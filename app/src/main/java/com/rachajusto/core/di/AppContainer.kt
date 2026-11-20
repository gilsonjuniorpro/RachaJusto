package com.rachajusto.core.di

import android.content.Context
import com.rachajusto.core.designsystem.theme.ThemePreferenceManager
import com.rachajusto.core.i18n.LanguageManager
import com.rachajusto.data.local.FileJsonGroupStorage
import com.rachajusto.data.network.MockNetworkSimulator
import com.rachajusto.data.repository.MockGroupRepository
import com.rachajusto.data.service.MockReceiptScannerService
import com.rachajusto.domain.repository.GroupRepository
import com.rachajusto.domain.service.ReceiptScannerService
import java.io.File

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val languageManager: LanguageManager by lazy {
        LanguageManager(appContext)
    }

    val themePreferenceManager: ThemePreferenceManager by lazy {
        ThemePreferenceManager(appContext)
    }

    val networkSimulator: MockNetworkSimulator by lazy {
        MockNetworkSimulator()
    }

    private val groupStorage: FileJsonGroupStorage by lazy {
        FileJsonGroupStorage(File(appContext.filesDir, FileJsonGroupStorage.FILE_NAME))
    }

    val groupRepository: GroupRepository by lazy {
        MockGroupRepository(
            storage = groupStorage,
            networkSimulator = networkSimulator
        )
    }

    val receiptScannerService: ReceiptScannerService by lazy {
        MockReceiptScannerService()
    }
}
