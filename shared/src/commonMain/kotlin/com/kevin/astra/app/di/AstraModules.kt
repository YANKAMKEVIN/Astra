package com.kevin.astra.app.di

import com.kevin.astra.core.navigation.AstraNavigator
import com.kevin.astra.presentation.assistant.AssistantViewModel
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.dsl.module

val astraRootModule = module {
    single { AstraNavigator() }
    single { AssistantViewModel() }
}

private val astraKoinApplication: KoinApplication by lazy {
    koinApplication {
        modules(astraRootModule)
    }
}

fun initializeKoin(): KoinApplication = astraKoinApplication
