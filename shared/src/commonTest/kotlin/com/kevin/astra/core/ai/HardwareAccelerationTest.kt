package com.kevin.astra.core.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HardwareAccelerationTest {

    @Test
    fun cpuIsTheDefaultAndUnknownValuesFallBackToIt() {
        assertEquals(HardwareAcceleration.Cpu, HardwareAcceleration.fromName(null))
        assertEquals(HardwareAcceleration.Cpu, HardwareAcceleration.fromName("TPU"))
        assertEquals(HardwareAcceleration.Gpu, HardwareAcceleration.fromName("Gpu"))
    }

    @Test
    fun everyAcceleratorFallsBackToCpu() {
        assertEquals(listOf(HardwareAcceleration.Cpu), HardwareAcceleration.Cpu.fallbackOrder)
        assertEquals(listOf(HardwareAcceleration.Gpu, HardwareAcceleration.Cpu), HardwareAcceleration.Gpu.fallbackOrder)
        assertEquals(listOf(HardwareAcceleration.Npu, HardwareAcceleration.Cpu), HardwareAcceleration.Npu.fallbackOrder)
    }

    @Test
    fun aWorkingGpuIsUsedWithoutAFallbackReason() {
        val result = initializeWithFallback(HardwareAcceleration.Gpu) { "engine on ${it.label}" }

        assertEquals("engine on GPU", result.engine)
        assertEquals(HardwareAcceleration.Gpu, result.acceleration)
        assertNull(result.fallbackReason)
    }

    @Test
    fun aFailingGpuFallsBackToCpuAndSaysWhy() {
        val tried = mutableListOf<HardwareAcceleration>()
        val failures = mutableListOf<HardwareAcceleration>()

        val result = initializeWithFallback(
            preferred = HardwareAcceleration.Gpu,
            onFailure = { acceleration, _ -> failures += acceleration },
        ) { acceleration ->
            tried += acceleration
            if (acceleration == HardwareAcceleration.Gpu) error("OpenCL not found") else "cpu engine"
        }

        assertEquals("cpu engine", result.engine)
        assertEquals(HardwareAcceleration.Cpu, result.acceleration)
        assertEquals(listOf(HardwareAcceleration.Gpu, HardwareAcceleration.Cpu), tried)
        assertEquals(listOf(HardwareAcceleration.Gpu), failures)
        assertTrue(result.fallbackReason.orEmpty().contains("GPU unavailable (OpenCL not found)"))
        assertTrue(result.fallbackReason.orEmpty().endsWith("ran on CPU."))
    }

    @Test
    fun cpuIsTriedOnlyOnceAndItsErrorIsRethrown() {
        var attempts = 0
        val error = assertFailsWith<IllegalStateException> {
            initializeWithFallback(HardwareAcceleration.Cpu) { attempts++; error("model file corrupt") }
        }
        assertEquals(1, attempts)
        assertEquals("model file corrupt", error.message)
    }

    @Test
    fun whenEveryAcceleratorFailsTheLastErrorIsRethrown() {
        val error = assertFailsWith<IllegalStateException> {
            initializeWithFallback(HardwareAcceleration.Npu) { error("${it.label} failed") }
        }
        assertEquals("CPU failed", error.message)
    }
}
