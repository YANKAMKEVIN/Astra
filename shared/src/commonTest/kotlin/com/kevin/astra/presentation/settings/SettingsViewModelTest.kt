package com.kevin.astra.presentation.settings

import com.kevin.astra.core.ai.PromptIndustry
import com.kevin.astra.data.ai.DefaultBackendCatalog
import com.kevin.astra.data.ai.DefaultModelCatalog
import com.kevin.astra.data.settings.testAiConfigurationRepository
import com.kevin.astra.domain.modelmanager.ModelDownloadManager
import com.kevin.astra.domain.modelmanager.ModelDownloadRequest
import com.kevin.astra.domain.modelmanager.ModelDownloadState
import com.kevin.astra.domain.modelmanager.ModelReadinessStatus
import com.kevin.astra.domain.modelmanager.StaticModelReadinessProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsViewModelTest {
    @Test
    fun exposesRepositoryDefaults() = runBlocking {
        val viewModel = testViewModel()
        delay(50)

        val state = viewModel.state.value

        assertEquals("mock-model", state.selectedModel?.id)
        assertTrue(state.availableModels.map { it.displayName }.containsAll(
            listOf("Mock Model", "Gemma 3 1B", "Phi-3 Mini", "Llama 3.2 3B", "Qwen 2.5 1.5B")
        ))
        assertEquals("mock-engine", state.selectedBackend?.id)
        assertTrue(state.modelReadiness.any { it.status == ModelReadinessStatus.Installed })
        assertTrue(state.modelReadiness.any { it.status == ModelReadinessStatus.UnsupportedPlatform || it.status == ModelReadinessStatus.ComingSoon })
        assertTrue(state.availableBackends.map { it.displayName }.containsAll(
            listOf("Mock Engine", "LiteRT", "LiteRT-LM")
        ))
        assertEquals(PromptIndustry.IndustrialMaintenance, state.selectedIndustry)
        assertEquals(0.3, state.temperature)
        assertEquals(512, state.maxTokens)
        assertEquals(4_096, state.contextWindow)
        assertEquals("4-bit", state.quantization)
        assertFalse(state.experimentalFeaturesEnabled)
    }

    @Test
    fun selectsAvailableModelAndInstalledBackendOnly() = runBlocking {
        val observationScope = CoroutineScope(coroutineContext + Job())
        val viewModel = testViewModel(observationScope)
        try {
            delay(50)

            viewModel.dispatch(SettingsIntent.SelectModel("gemma-3-1b"))
            viewModel.dispatch(SettingsIntent.SelectBackend("onnx-runtime"))
            delay(150)

            val state = viewModel.state.value
            assertEquals("gemma-3-1b", state.selectedModel?.id)
            assertEquals("mock-engine", state.selectedBackend?.id)

            viewModel.dispatch(SettingsIntent.SelectBackend("litert-lm"))
            delay(150)
            assertEquals("litert-lm", viewModel.state.value.selectedBackend?.id)
        } finally {
            observationScope.cancel()
        }
    }

    @Test
    fun updatesParametersAndExperimentalToggle() = runBlocking {
        val observationScope = CoroutineScope(coroutineContext + Job())
        val viewModel = testViewModel(observationScope)
        try {
            delay(50)

            viewModel.dispatch(SettingsIntent.SelectIndustry(PromptIndustry.Healthcare))
            viewModel.dispatch(SettingsIntent.UpdateTemperature(0.6))
            viewModel.dispatch(SettingsIntent.UpdateMaxTokens(1_024))
            viewModel.dispatch(SettingsIntent.UpdateContextWindow(8_192))
            viewModel.dispatch(SettingsIntent.UpdateQuantization("8-bit"))
            viewModel.dispatch(SettingsIntent.ToggleExperimentalFeatures(true))
            delay(150)

            val state = viewModel.state.value
            assertEquals(PromptIndustry.Healthcare, state.selectedIndustry)
            assertEquals(0.6, state.temperature)
            assertEquals(1_024, state.maxTokens)
            assertEquals(8_192, state.contextWindow)
            assertEquals("8-bit", state.quantization)
            assertTrue(state.experimentalFeaturesEnabled)
        } finally {
            observationScope.cancel()
        }
    }

    @Test
    fun togglesDemoMode() = runBlocking {
        val observationScope = CoroutineScope(coroutineContext + Job())
        val viewModel = testViewModel(observationScope)
        try {
            delay(50)
            assertFalse(viewModel.state.value.demoModeEnabled)

            viewModel.dispatch(SettingsIntent.ToggleDemoMode(true))
            delay(150)
            assertTrue(viewModel.state.value.demoModeEnabled)

            viewModel.dispatch(SettingsIntent.ToggleDemoMode(false))
            delay(150)
            assertFalse(viewModel.state.value.demoModeEnabled)
        } finally {
            observationScope.cancel()
        }
    }

    private fun testViewModel(
        observationScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext),
    ): SettingsViewModel =
        SettingsViewModel(
            aiConfigurationRepository = testAiConfigurationRepository(),
            modelCatalog = DefaultModelCatalog(),
            backendCatalog = DefaultBackendCatalog(),
            modelReadinessProvider = StaticModelReadinessProvider(platformName = "Test"),
            modelDownloadManager = NoOpModelDownloadManager(),
            observationScope = observationScope,
        )
}

private class NoOpModelDownloadManager : ModelDownloadManager {
    override val downloadState: StateFlow<ModelDownloadState> =
        MutableStateFlow(ModelDownloadState.Idle)
    override suspend fun download(request: ModelDownloadRequest) = Unit
    override fun cancel(modelId: String) = Unit
    override fun deleteModel(modelId: String): Boolean = false
    override fun getInstalledModelPaths(): Map<String, String> = emptyMap()
    override fun getStorageUsageMb(): Float = 0f
}
