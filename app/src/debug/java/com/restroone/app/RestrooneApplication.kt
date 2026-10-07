package com.restroone.app

import android.app.Application
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions

class RestrooneApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseAppCheck.getInstance()
            .installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())

        BuildConfig.FIREBASE_EMULATOR_HOST.takeIf(String::isNotBlank)?.let { host ->
            FirebaseAuth.getInstance().useEmulator(host, 9099)
            FirebaseFirestore.getInstance().useEmulator(host, 18655)
            FirebaseFunctions.getInstance().useEmulator(host, 5001)
        }
    }
}
