package com.fourthwardai.orbit

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class OrbitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Use BuildConfig.DEBUG without package qualifier so we don't refer to the old package name
        // if (BuildConfig.DEBUG) {
        Timber.plant(Timber.DebugTree())
        // }
    }
}
