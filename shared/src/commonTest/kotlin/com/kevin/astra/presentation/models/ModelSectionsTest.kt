package com.kevin.astra.presentation.models

import com.kevin.astra.data.ai.DefaultModelCatalog
import com.kevin.astra.domain.modelmanager.ModelDownloadState
import com.kevin.astra.domain.modelmanager.ModelReadiness
import com.kevin.astra.domain.modelmanager.ModelReadinessStatus
import com.kevin.astra.domain.modelmanager.StaticModelReadinessProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModelSectionsTest {
    private val models = DefaultModelCatalog().availableModels()

    private fun readiness(installed: Set<String> = setOf("mock-model")): List<ModelReadiness> =
        StaticModelReadinessProvider(platformName = "iOS")
            .readinessFor(models)
            .map { r ->
                r.copy(
                    status = if (r.modelId in installed) ModelReadinessStatus.Installed else ModelReadinessStatus.ModelRequired,
                )
            }

    private fun ModelSections.allIds(): List<String> =
        (onDevice + downloading + tiers.flatMap { it.entries } + tooLarge).map { it.id }

    @Test
    fun everyModelLandsInExactlyOneSection() {
        val sections = buildModelSections(
            readiness = readiness(),
            models = models,
            selectedModelId = "mock-model",
            downloadState = ModelDownloadState.Idle,
            deviceMemoryMb = 6_144,
        )

        val ids = sections.allIds()
        assertEquals(models.size, ids.size)
        assertEquals(models.map { it.id }.toSet(), ids.toSet())
    }

    @Test
    fun installedModelsComeFirstWithTheActiveOneOnTop() {
        val sections = buildModelSections(
            readiness = readiness(installed = setOf("mock-model", "qwen3-0-6b", "gemma-4-e2b")),
            models = models,
            selectedModelId = "gemma-4-e2b",
            downloadState = ModelDownloadState.Idle,
            deviceMemoryMb = 8_192,
        )

        assertEquals("gemma-4-e2b", sections.onDevice.first().id)
        assertTrue(sections.onDevice.first().isActive)
        assertEquals(setOf("mock-model", "qwen3-0-6b", "gemma-4-e2b"), sections.onDevice.map { it.id }.toSet())
        assertTrue(sections.tiers.flatMap { it.entries }.none { it.isInstalled })
    }

    @Test
    fun catalogIsGroupedByRamTierSmallestFirst() {
        val sections = buildModelSections(
            readiness = readiness(),
            models = models,
            selectedModelId = "mock-model",
            downloadState = ModelDownloadState.Idle,
            deviceMemoryMb = 16_384,
        )

        assertEquals(listOf(ModelTier.Lightweight, ModelTier.Balanced, ModelTier.Powerful), sections.tiers.map { it.tier })
        sections.tiers.forEach { section ->
            section.entries.forEach { entry ->
                assertEquals(section.tier, ModelTier.forMemory(entry.minimumMemoryMb), "${entry.id} in wrong tier")
            }
        }
        val light = sections.tiers.first { it.tier == ModelTier.Lightweight }.entries.map { it.id }
        assertTrue("smollm2-135m" in light && "qwen3-0-6b" in light)
        assertTrue(sections.tooLarge.isEmpty())
    }

    @Test
    fun downloadableModelsAreListedBeforeManualInstallsWithinATier() {
        val sections = buildModelSections(
            readiness = readiness(),
            models = models,
            selectedModelId = "mock-model",
            downloadState = ModelDownloadState.Idle,
            deviceMemoryMb = 16_384,
        )

        sections.tiers.forEach { section ->
            val flags = section.entries.map { it.isDownloadable }
            assertEquals(flags.sortedDescending(), flags, "Manual installs should trail in ${section.tier}")
        }
    }

    @Test
    fun modelsNeedingMoreRamThanTheDeviceAreFoldedAway() {
        val sections = buildModelSections(
            readiness = readiness(),
            models = models,
            selectedModelId = "mock-model",
            downloadState = ModelDownloadState.Idle,
            deviceMemoryMb = 4_096,
        )

        val tooLarge = sections.tooLarge.map { it.id }.toSet()
        assertTrue("gemma-4-12b" in tooLarge && "codegemma-7b" in tooLarge && "gemma-4-e4b" in tooLarge)
        assertTrue(sections.tooLarge.all { it.minimumMemoryMb > 4_096 })
        assertTrue(sections.tiers.flatMap { it.entries }.all { it.minimumMemoryMb <= 4_096 })
    }

    @Test
    fun unknownDeviceMemoryFlagsNothingAsTooLarge() {
        val sections = buildModelSections(
            readiness = readiness(),
            models = models,
            selectedModelId = "mock-model",
            downloadState = ModelDownloadState.Idle,
            deviceMemoryMb = null,
        )

        assertTrue(sections.tooLarge.isEmpty())
    }

    @Test
    fun theInFlightDownloadGetsItsOwnSection() {
        val sections = buildModelSections(
            readiness = readiness(),
            models = models,
            selectedModelId = "mock-model",
            downloadState = ModelDownloadState.Downloading("qwen3-1-7b", 40, 400f, 1_000f),
            deviceMemoryMb = 8_192,
        )

        assertEquals(listOf("qwen3-1-7b"), sections.downloading.map { it.id })
        assertEquals(40, sections.downloading.single().downloading?.progressPercent)
        assertTrue(sections.tiers.flatMap { it.entries }.none { it.id == "qwen3-1-7b" })
    }

    @Test
    fun searchMatchesNameOrProviderCaseInsensitively() {
        val byName = buildModelSections(readiness(), models, "mock-model", ModelDownloadState.Idle, 16_384, query = "smollm")
        assertEquals(setOf("smollm2-135m", "smollm2-360m", "smollm3-3b"), byName.allIds().toSet())

        val byProvider = buildModelSections(readiness(), models, "mock-model", ModelDownloadState.Idle, 16_384, query = "META")
        assertEquals(setOf("llama-3-2-1b", "llama-3-2-3b"), byProvider.allIds().toSet())

        val none = buildModelSections(readiness(), models, "mock-model", ModelDownloadState.Idle, 16_384, query = "zzz")
        assertTrue(none.isEmpty)
    }
}
