package com.example

import android.app.Application
import com.example.data.VTDatabase
import com.example.player.VTPlayerManager

class VTPlayerApplication : Application() {
    val database: VTDatabase by lazy { VTDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: VTPlayerApplication
            private set
    }
}

