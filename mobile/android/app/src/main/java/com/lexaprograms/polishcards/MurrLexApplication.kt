package com.lexaprograms.polishcards

import android.app.Application
import app.rive.runtime.kotlin.core.Rive

class MurrLexApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogStore.install(this)
        runCatching { Rive.init(this) }
            .onFailure { CrashLogStore.record(this, it, "main") }
    }
}
