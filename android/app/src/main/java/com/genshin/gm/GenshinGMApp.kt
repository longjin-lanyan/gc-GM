package com.genshin.gm

import android.app.Application

class GenshinGMApp : Application() {
    companion object {
        lateinit var instance: GenshinGMApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
