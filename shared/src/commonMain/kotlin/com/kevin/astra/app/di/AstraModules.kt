package com.kevin.astra.app.di

import com.kevin.astra.core.navigation.AstraNavigator
import org.koin.core.KoinApplication
import org.koin.dsl.module
import org.koin.dsl.koinApplication

val astraRootModule = module {
    single { AstraNavigator() }
}

private val astraKoinApplication: KoinApplication by lazy {
    koinApplication {
        modules(astraRootModule)
    }
}

fun initializeKoin(): KoinApplication = astraKoinApplication
