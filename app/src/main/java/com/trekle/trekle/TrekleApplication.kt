package com.trekle.trekle

import android.app.Application
import android.content.Context

class TrekleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }

    companion object {
        var appContext: Context? = null
            private set
    }
}