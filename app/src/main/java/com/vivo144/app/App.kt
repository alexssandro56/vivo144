package com.vivo144.app

import android.app.Application
import com.vivo144.app.core.Rms
import com.vivo144.app.core.Prefs
import com.vivo144.app.core.ShizukuState

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Rms.app = this
        Prefs.init(this)
        ShizukuState.init()
    }
}
