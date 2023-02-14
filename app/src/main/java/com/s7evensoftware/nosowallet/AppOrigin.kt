package com.s7evensoftware.nosowallet

import android.app.Application
import com.s7evensoftware.nosowallet.nosocore.mpDisk
import com.s7evensoftware.nosowallet.util.Log

class AppOrigin: Application() {

    override fun onCreate() {
        super.onCreate()
        mpDisk.setContext(this)

        // UncaughtHandler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        // UncaughtHandler
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            Log.e("NOSOmobile","Unhandled error: ${e.message}")
            defaultHandler?.uncaughtException(t, e)
        }
    }
}