package com.kevin.astra.app.di

import android.content.Context
import org.koin.core.context.GlobalContext

/** The Android application context, provided by Koin's androidContext() at startup. */
internal fun androidAppContext(): Context = GlobalContext.get().get()
