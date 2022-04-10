package com.s7evensoftware.nosowallet

import android.app.Application
import io.realm.Realm

class AppOrigin: Application() {

    override fun onCreate() {
        super.onCreate()
        mpDisk.setContext(this)
        Realm.init(this)

        // UncaughtHandler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        // UncaughtHandler
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            Log.e("NOOSOmobile","Unhandled error: ${e.message}")
            defaultHandler.uncaughtException(t, e)
        }
    }
}