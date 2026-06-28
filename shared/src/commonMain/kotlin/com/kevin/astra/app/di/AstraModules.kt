package com.kevin.astra.app.di

import com.kevin.astra.core.ai.BackendCatalog
import com.kevin.astra.core.ai.DefaultPromptBuilder
import com.kevin.astra.core.ai.DefaultPromptPipeline
import com.kevin.astra.core.ai.InferenceEngine
import com.kevin.astra.core.ai.ModelCatalog
import com.kevin.astra.core.ai.PromptBuilder
import com.kevin.astra.core.ai.PromptPipeline
import com.kevin.astra.core.ai.createInferenceEngine
import com.kevin.astra.core.device.DeviceCapabilityProvider
import com.kevin.astra.core.device.createDeviceCapabilityProvider
import com.kevin.astra.core.navigation.AstraNavigator
import com.kevin.astra.core.notification.createNotificationService
import com.kevin.astra.data.ai.DefaultModelCatalog
import com.kevin.astra.data.ai.createBackendCatalog
import com.kevin.astra.data.benchmark.RuntimeBenchmarkRunner
import com.kevin.astra.data.demo.StaticDemoScenarioCatalog
import com.kevin.astra.data.documents.KeywordDocumentContextRetriever
import com.kevin.astra.data.documents.SimpleDocumentIndexer
import com.kevin.astra.data.settings.AiConfigurationLocalDataSource
import com.kevin.astra.data.settings.PersistentAiConfigurationRepository
import com.kevin.astra.data.settings.createAiConfigurationKeyValueStore
import com.kevin.astra.domain.assistant.AskLocalAssistantUseCase
import com.kevin.astra.domain.benchmark.BenchmarkRunner
import com.kevin.astra.domain.demo.DemoScenarioCatalog
import com.kevin.astra.domain.documents.DocumentContextRetriever
import com.kevin.astra.domain.documents.DocumentIndexer
import com.kevin.astra.domain.modelmanager.ModelReadinessProvider
import com.kevin.astra.domain.modelmanager.createModelReadinessProvider
import com.kevin.astra.domain.settings.AiConfigurationRepository
import com.kevin.astra.presentation.assistant.AssistantViewModel
import com.kevin.astra.presentation.benchmark.BenchmarkViewModel
import com.kevin.astra.presentation.dashboard.DashboardViewModel
import com.kevin.astra.presentation.demo.DemoViewModel
import com.kevin.astra.presentation.documents.DocumentsViewModel
import com.kevin.astra.presentation.overview.ProjectOverviewViewModel
import com.kevin.astra.presentation.settings.SettingsViewModel
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.module

val astraRootModule = module {
    single { AstraNavigator() }
    single<ModelCatalog> { DefaultModelCatalog() }
    single<BackendCatalog> { createBackendCatalog() }
    single<DeviceCapabilityProvider> { createDeviceCapabilityProvider() }
    single { createNotificationService() }
    single { AiConfigurationLocalDataSource(keyValueStore = createAiConfigurationKeyValueStore()) }
    single<PromptBuilder> { DefaultPromptBuilder() }
    single<PromptPipeline> { DefaultPromptPipeline(promptBuilder = get()) }
    single<AiConfigurationRepository> { PersistentAiConfigurationRepository(localDataSource = get()) }
    single<InferenceEngine> { createInferenceEngine() }
    single<BenchmarkRunner> { RuntimeBenchmarkRunner(inferenceEngine = get()) }
    single<ModelReadinessProvider> { createModelReadinessProvider() }
    single<DemoScenarioCatalog> { StaticDemoScenarioCatalog() }
    single<DocumentIndexer> { SimpleDocumentIndexer() }
    single<DocumentContextRetriever> { KeywordDocumentContextRetriever() }
    single { AskLocalAssistantUseCase(inferenceEngine = get()) }
    single { DashboardViewModel(deviceCapabilityProvider = get()) }
    single {
        DemoViewModel(
            deviceCapabilityProvider = get(),
            aiConfigurationRepository = get(),
            modelCatalog = get(),
            backendCatalog = get(),
            modelReadinessProvider = get(),
            demoScenarioCatalog = get(),
        )
    }
    single {
        ProjectOverviewViewModel(
            deviceCapabilityProvider = get(),
            aiConfigurationRepository = get(),
            modelCatalog = get(),
            backendCatalog = get(),
            modelReadinessProvider = get(),
        )
    }
    single {
        AssistantViewModel(
            askLocalAssistant = get(),
            aiConfigurationRepository = get(),
            modelCatalog = get(),
            backendCatalog = get(),
            promptPipeline = get(),
            demoScenarioCatalog = get(),
            notificationService = get(),
        )
    }
    single {
        BenchmarkViewModel(
            benchmarkRunner = get(),
            modelCatalog = get(),
            backendCatalog = get(),
            aiConfigurationRepository = get(),
            promptPipeline = get(),
            demoScenarioCatalog = get(),
            notificationService = get(),
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
            notificationService = get(),
        )
    }
    single {
        SettingsViewModel(
            aiConfigurationRepository = get(),
            modelCatalog = get(),
            backendCatalog = get(),
            modelReadinessProvider = get(),
        )
    }
}

private var koinApp: KoinApplication? = null

fun initializeKoin(): KoinApplication {
    if (koinApp == null) {
        koinApp = startKoin {
            modules(astraRootModule)
        }
    }
    return koinApp!!
}
