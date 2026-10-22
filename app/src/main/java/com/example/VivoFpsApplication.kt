package com.example

import android.app.Application
import com.example.data.AppContainer

class VivoFpsApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        appContainer = AppContainer(this)
    }

    companion object {
        lateinit var instance: VivoFpsApplication
            private set
    }
}
