package com.modih.mail

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class ModihMailApp : Application() {
    override fun onCreate() {
        super.onCreate()
        runCatching {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        }.onFailure { error ->
            Log.e("ModihMailApp", "Firebase initialization failed", error)
        }
    }
}
