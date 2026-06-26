package com.kevin.astra.app.di

import com.kevin.astra.core.ai.BackendCatalog
import com.kevin.astra.core.ai.InferenceEngine
import com.kevin.astra.core.ai.ModelCatalog
import com.kevin.astra.core.ai.MockInferenceEngine
import com.kevin.astra.core.ai.DefaultPromptBuilder
import com.kevin.astra.core.ai.DefaultPromptPipeline
import com.kevin.astra.core.ai.PromptBuilder
import com.kevin.astra.core.ai.PromptPipeline
import com.kevin.astra.core.device.DeviceCapabilityProvider
import com.kevin.astra.core.device.createDeviceCapabilityProvider
import com.kevin.astra.core.navigation.AstraNavigator
import com.kevin.astra.data.ai.DefaultBackendCatalog
import com.kevin.astra.data.ai.DefaultModelCatalog
import com.kevin.astra.data.benchmark.MockBenchmarkRunner
import com.kevin.astra.data.documents.KeywordDocumentContextRetriever
import com.kevin.astra.data.documents.SimpleDocumentIndexer
import com.kevin.astra.data.settings.InMemoryAiConfigurationRepository
import com.kevin.astra.domain.assistant.AskLocalAssistantUseCase
import com.kevin.astra.domain.benchmark.BenchmarkRunner
import com.kevin.astra.domain.documents.DocumentContextRetriever
import com.kevin.astra.domain.documents.DocumentIndexer
import com.kevin.astra.domain.settings.AiConfigurationRepository
import com.kevin.astra.presentation.assistant.AssistantViewModel
import com.kevin.astra.presentation.benchmark.BenchmarkViewModel
import com.kevin.astra.presentation.dashboard.DashboardViewModel
import com.kevin.astra.presentation.documents.DocumentsViewModel
import com.kevin.astra.presentation.settings.SettingsViewModel
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.dsl.module

val astraRootModule = module {
    single { AstraNavigator() }
    single<ModelCatalog> { DefaultModelCatalog() }
    single<BackendCatalog> { DefaultBackendCatalog() }
    single<DeviceCapabilityProvider> { createDeviceCapabilityProvider() }
    single<PromptBuilder> { DefaultPromptBuilder() }
    single<PromptPipeline> { DefaultPromptPipeline(promptBuilder = get()) }
    single<AiConfigurationRepository> { InMemoryAiConfigurationRepository() }
    single<InferenceEngine> { MockInferenceEngine() }
    single<BenchmarkRunner> { MockBenchmarkRunner() }
    single<DocumentIndexer> { SimpleDocumentIndexer() }
    single<DocumentContextRetriever> { KeywordDocumentContextRetriever() }
    single { AskLocalAssistantUseCase(inferenceEngine = get()) }
    single { DashboardViewModel(deviceCapabilityProvider = get()) }
    single {
        AssistantViewModel(
            askLocalAssistant = get(),
            aiConfigurationRepository = get(),
            modelCatalog = get(),
            backendCatalog = get(),
            promptPipeline = get(),
        )
    }
    single {
        BenchmarkViewModel(
            benchmarkRunner = get(),
            modelCatalog = get(),
            backendCatalog = get(),
            aiConfigurationRepository = get(),
            promptPipeline = get(),
        )
    }
    single {
        DocumentsViewModel(
            documentIndexer = get(),
            contextRetriever = get(),
            askLocalAssistant = get(),
            aiConfigurationRepository = get(),
            modelCatalog = get(),
            backendCatalog = get(),
            promptPipeline = get(),
        )
    }
    single {
        SettingsViewModel(
            aiConfigurationRepository = get(),
            modelCatalog = get(),
            backendCatalog = get(),
        )
    }
}

private val astraKoinApplication: KoinApplication by lazy {
    koinApplication {
        modules(astraRootModule)
    }
}

fun initializeKoin(): KoinApplication = astraKoinApplication
