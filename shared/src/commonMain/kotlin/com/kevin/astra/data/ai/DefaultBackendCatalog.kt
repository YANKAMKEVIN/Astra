package com.kevin.astra.data.ai

import com.kevin.astra.core.ai.AccelerationTarget
import com.kevin.astra.core.ai.BackendCatalog
import com.kevin.astra.core.ai.BackendProvider
import com.kevin.astra.core.ai.BackendStatus
import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.ai.InferenceBackendInfo

/**
 * Catalog of the backends ASTRA actually ships: the deterministic [InferenceBackend.Mock] engine
 * and the real [InferenceBackend.LiteRtLm] runtime (MediaPipe on Android, the LiteRTLMSwift bridge
 * on iOS). Aspirational backends (plain LiteRT tensor, ONNX, Core ML, llama.cpp) were removed from
 * the picker since no user-facing runtime uses them.
 *
 * [statusOverrides] maps a backend id to a **status provider** rather than a fixed status so a
 * platform can report the *live* readiness of its native runtime — e.g. iOS reports LiteRT-LM as
 * installed only while its Swift bridge is registered, instead of asserting it unconditionally.
 */
class DefaultBackendCatalog(
    private val statusOverrides: Map<String, () -> BackendStatus> = emptyMap(),
) : BackendCatalog {
    private val definitions = listOf(
        InferenceBackendInfo(
            id = "mock-engine",
            displayName = "Mock Engine",
            provider = BackendProvider.Mock,
            supportedPlatforms = listOf("Android", "iOS"),
            supportedModelFormats = listOf("Simulated"),
            accelerationTargets = listOf(AccelerationTarget.Cpu),
            status = BackendStatus.Installed,
            description = "Deterministic local mock backend used for offline development and demos.",
            runtimeBackend = InferenceBackend.Mock,
        ),
        InferenceBackendInfo(
            id = "litert-lm",
            displayName = "LiteRT-LM",
            provider = BackendProvider.Google,
            supportedPlatforms = listOf("Android", "iOS"),
            supportedModelFormats = listOf("LiteRT-LM bundle", "TFLite", "SentencePiece tokenizer"),
            accelerationTargets = listOf(AccelerationTarget.Cpu, AccelerationTarget.Gpu, AccelerationTarget.Nnapi, AccelerationTarget.Npu),
            status = BackendStatus.Installed,
            description = "On-device GenAI runtime for real local LiteRT-LM text generation when a compatible model bundle is present.",
            runtimeBackend = InferenceBackend.LiteRtLm,
        ),
    )

    private var currentBackendId: String = preferredDefaultBackend().id

    /** Resolves each backend's status against its live provider (if any) at call time. */
    private fun resolvedBackends(): List<InferenceBackendInfo> =
        definitions.map { definition ->
            val override = statusOverrides[definition.id] ?: return@map definition
            definition.copy(status = override())
        }

    override fun availableBackends(): List<InferenceBackendInfo> = resolvedBackends()

    override fun installedBackends(): List<InferenceBackendInfo> =
        resolvedBackends().filter { it.status == BackendStatus.Installed }

    override fun currentBackend(): InferenceBackendInfo =
        resolvedBackends().firstOrNull { it.id == currentBackendId } ?: preferredDefaultBackend()

    override fun selectBackend(backendId: String): Boolean {
        val backend = resolvedBackends().firstOrNull { it.id == backendId } ?: return false
        if (backend.status != BackendStatus.Installed) return false
        currentBackendId = backend.id
        return true
    }

    override fun backendById(backendId: String): InferenceBackendInfo? =
        resolvedBackends().firstOrNull { it.id == backendId }

    override fun preferredDefaultBackend(): InferenceBackendInfo {
        val resolved = resolvedBackends()
        return resolved.firstOrNull {
            it.runtimeBackend == InferenceBackend.LiteRtLm && it.status == BackendStatus.Installed
        }
            ?: resolved.firstOrNull { it.status == BackendStatus.Installed }
            ?: resolved.first()
    }
}
