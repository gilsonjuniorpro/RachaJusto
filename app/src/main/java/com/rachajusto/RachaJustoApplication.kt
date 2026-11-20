package com.rachajusto

import android.app.Application
import com.rachajusto.core.di.AppContainer

class RachaJustoApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
