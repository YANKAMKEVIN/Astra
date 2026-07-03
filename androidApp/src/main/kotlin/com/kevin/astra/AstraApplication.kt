package com.kevin.astra

import android.app.Application
import com.kevin.astra.app.di.initializeKoin
import org.koin.android.ext.koin.androidContext

/**
 * Starts Koin with the Android application context registered (androidContext), at the
 * guaranteed-earliest entry point. Android platform services obtain the context from Koin, so the
 * scattered initializeAndroidXxx(context) calls and their global mutable vars are gone.
 */
class AstraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeKoin { androidContext(this@AstraApplication) }
    }
}
