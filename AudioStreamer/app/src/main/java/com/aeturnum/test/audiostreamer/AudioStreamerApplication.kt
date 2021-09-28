package com.aeturnum.test.audiostreamer

import android.app.Application
import com.tinder.scarlet.lifecycle.android.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber


@HiltAndroidApp
class AudioStreamerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
