package com.kevin.astra.data.settings

import com.kevin.astra.core.ai.HardwareAcceleration
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class HardwareAccelerationSettingsTest {

    @Test
    fun cpuIsTheDefaultOnAFreshInstall() = runBlocking {
        assertEquals(HardwareAcceleration.Cpu, testAiConfigurationRepository().getConfiguration().hardwareAcceleration)
    }

    @Test
    fun theChosenAcceleratorSurvivesARestart() = runBlocking {
        val store = TestAiConfigurationKeyValueStore()
        val repository = testAiConfigurationRepository(keyValueStore = store)

        repository.updateConfiguration(repository.getConfiguration().copy(hardwareAcceleration = HardwareAcceleration.Gpu))

        assertEquals(
            HardwareAcceleration.Gpu,
            testAiConfigurationRepository(keyValueStore = store).getConfiguration().hardwareAcceleration,
        )
    }

    @Test
    fun anUnknownSavedValueFallsBackToCpu() = runBlocking {
        val store = TestAiConfigurationKeyValueStore().apply { putString("ai.hardware_acceleration", "QuantumCore") }

        assertEquals(HardwareAcceleration.Cpu, testAiConfigurationRepository(keyValueStore = store).getConfiguration().hardwareAcceleration)
    }
}
