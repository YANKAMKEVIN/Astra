package com.kevin.astra.app.di

import com.kevin.astra.core.ai.InferenceEngine
import com.kevin.astra.core.ai.MockInferenceEngine
import com.kevin.astra.core.navigation.AstraNavigator
import com.kevin.astra.data.settings.InMemoryAiConfigurationRepository
import com.kevin.astra.domain.assistant.AskLocalAssistantUseCase
import com.kevin.astra.domain.settings.AiConfigurationRepository
import com.kevin.astra.presentation.assistant.AssistantViewModel
import com.kevin.astra.presentation.settings.SettingsViewModel
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.dsl.module

val astraRootModule = module {
    single { AstraNavigator() }
    single<AiConfigurationRepository> { InMemoryAiConfigurationRepository() }
    single<InferenceEngine> { MockInferenceEngine() }
    single { AskLocalAssistantUseCase(inferenceEngine = get()) }
    single {
        AssistantViewModel(
            askLocalAssistant = get(),
            aiConfigurationRepository = get(),
        )
    }
    single { SettingsViewModel(aiConfigurationRepository = get()) }
}

private val astraKoinApplication: KoinApplication by lazy {
    koinApplication {
        modules(astraRootModule)
    }
}

fun initializeKoin(): KoinApplication = astraKoinApplication
