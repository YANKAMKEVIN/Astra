package com.kevin.astra.presentation.models

import com.kevin.astra.core.ai.LocalModel
import com.kevin.astra.domain.modelmanager.ModelDownloadState
import com.kevin.astra.domain.modelmanager.ModelReadiness
import com.kevin.astra.domain.modelmanager.ModelReadinessStatus

/** Catalog size tiers, keyed on the RAM a model needs — what matters on a phone. */
enum class ModelTier(val title: String, val subtitle: String, val maxMemoryMb: Int) {
    Lightweight("Lightweight", "Up to 1 GB RAM · fastest", 1_024),
    Balanced("Balanced", "1–2.5 GB RAM · everyday use", 2_560),
    Powerful("Powerful", "3 GB+ RAM · best quality", Int.MAX_VALUE),
    ;

    companion object {
        fun forMemory(minimumMemoryMb: Int): ModelTier =
            entries.first { minimumMemoryMb <= it.maxMemoryMb }
    }
}

/** One catalog entry, with everything the Models screen needs to render and act on it. */
data class ModelEntry(
    val readiness: ModelReadiness,
    val model: LocalModel?,
    val isActive: Boolean,
    val downloading: ModelDownloadState.Downloading?,
) {
    val id: String get() = readiness.modelId
    val isInstalled: Boolean get() = readiness.status == ModelReadinessStatus.Installed
    val isDownloadable: Boolean get() = model?.downloadUrl != null
    val minimumMemoryMb: Int get() = model?.minimumMemoryMb ?: 0
}

data class ModelTierSection(val tier: ModelTier, val entries: List<ModelEntry>)

data class ModelSections(
    val onDevice: List<ModelEntry>,
    val downloading: List<ModelEntry>,
    val tiers: List<ModelTierSection>,
    val tooLarge: List<ModelEntry>,
) {
    val isEmpty: Boolean
        get() = onDevice.isEmpty() && downloading.isEmpty() && tiers.isEmpty() && tooLarge.isEmpty()
}

/**
 * Splits the catalog into the sections of the Models screen:
 * installed models first (active one on top), the in-flight download, then the
 * rest grouped by [ModelTier] (smallest first), and finally the models whose RAM
 * requirement exceeds the device's memory. [deviceMemoryMb] of null means
 * "unknown" — nothing is flagged as too large.
 */
fun buildModelSections(
    readiness: List<ModelReadiness>,
    models: List<LocalModel>,
    selectedModelId: String?,
    downloadState: ModelDownloadState,
    deviceMemoryMb: Long?,
    query: String = "",
): ModelSections {
    val downloading = downloadState as? ModelDownloadState.Downloading
    val entries = readiness
        .map { r ->
            ModelEntry(
                readiness = r,
                model = models.firstOrNull { it.id == r.modelId },
                isActive = r.modelId == selectedModelId,
                downloading = downloading?.takeIf { it.modelId == r.modelId },
            )
        }
        .filter { it.matches(query) }

    val onDevice = entries
        .filter { it.isInstalled }
        .sortedWith(compareByDescending<ModelEntry> { it.isActive }.thenBy { it.minimumMemoryMb })
    val inFlight = entries.filter { !it.isInstalled && it.downloading != null }
    val catalog = entries.filter { !it.isInstalled && it.downloading == null }

    val (fits, tooLarge) = catalog.partition { entry ->
        deviceMemoryMb == null || deviceMemoryMb <= 0 || entry.minimumMemoryMb <= deviceMemoryMb
    }
    val tiers = fits
        .groupBy { ModelTier.forMemory(it.minimumMemoryMb) }
        .map { (tier, list) -> ModelTierSection(tier, list.sortedForCatalog()) }
        .sortedBy { it.tier.ordinal }

    return ModelSections(
        onDevice = onDevice,
        downloading = inFlight,
        tiers = tiers,
        tooLarge = tooLarge.sortedBy { it.minimumMemoryMb },
    )
}

// One-tap downloads before models that need a manual install, then smallest first.
private fun List<ModelEntry>.sortedForCatalog(): List<ModelEntry> =
    sortedWith(compareByDescending<ModelEntry> { it.isDownloadable }.thenBy { it.minimumMemoryMb })

private fun ModelEntry.matches(query: String): Boolean =
    query.isBlank() ||
        readiness.displayName.contains(query, ignoreCase = true) ||
        readiness.provider.contains(query, ignoreCase = true)
