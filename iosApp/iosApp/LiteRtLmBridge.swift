import Foundation
import LiteRTLMSwift
import Shared

/// Swift-side implementation of the Kotlin `NativeLiteRtLmEngine` interface
/// (shared/src/iosMain/kotlin/com/kevin/astra/core/ai/IosLiteRtLmRuntime.kt).
///
/// Wraps the community `LiteRTLMSwift` package (https://github.com/mylovelycodes/LiteRTLM-Swift),
/// which ships a prebuilt `CLiteRTLM.xcframework` around Google's LiteRT-LM C API. We switched to
/// this fork after Google's own official Swift package (google-ai-edge/LiteRT-LM) failed to build:
/// tracking `main` hit a Bazel packaging bug (missing `ios_engine.bzl`), and pinning the `0.14.0`
/// release tag hit SwiftPM's "unsafe build flags" restriction on versioned dependencies, then a
/// single unresolved C symbol in an unused debug/logging method (`renderPrefaceIntoString`) even
/// after pinning to that release's exact commit. See docs/10_iOS_LiteRT_LM_Setup.md for the full
/// story. The fork's prebuilt binary sidesteps all three problems.
///
/// NOTE: this file depends on the `LiteRTLMSwift` Swift package (product name `LiteRTLMSwift`,
/// NOT `LiteRTLM`), added via File > Add Package Dependencies... using the URL
/// `https://github.com/mylovelycodes/LiteRTLM-Swift.git`. It also requires the
/// "Increased Memory Limit" capability under Signing & Capabilities (model loading needs ~4 GB
/// RAM) — see docs/10_iOS_LiteRT_LM_Setup.md. Neither step can be done by editing project.pbxproj
/// by hand from this environment — do them once in Xcode.
///
/// The exact Swift-visible name Kotlin/Native generates for the `NativeLiteRtLmEngine` interface
/// and `NativeLiteRtLmResult` data class should match 1:1 (Kotlin/Native keeps interface/class
/// names unchanged when exporting to Swift). If Xcode reports a different name, Cmd-click into
/// `Shared` to check the generated header and adjust below.
final class LiteRtLmBridge: NSObject {
    private var engine: LiteRTLMEngine?
    private var activeModelPath: String?

    /// Re-creates the engine only when the requested model path changes, mirroring the
    /// session-reuse behavior of `AndroidLiteRtLmRuntimeSession.ensureInference`.
    private func ensureEngine(modelPath: String) async throws -> LiteRTLMEngine {
        if let engine, activeModelPath == modelPath {
            return engine
        }

        // "cpu" is the safe default (works on Simulator and all devices). The fork's README
        // marks "gpu" (Metal) as experimental — switch once validated on a physical device.
        let newEngine = LiteRTLMEngine(modelPath: URL(fileURLWithPath: modelPath), backend: "cpu")
        try await newEngine.load()

        engine = newEngine
        activeModelPath = modelPath
        return newEngine
    }

    /// The fork's Session API (`generate`) requires Gemma 4's native turn-marker format rather
    /// than a plain string — see the fork's README "Gemma 4 Prompt Format" section. Our Kotlin
    /// PromptPipeline builds a plain instruction string, so we wrap it here rather than changing
    /// the shared Kotlin request shape.
    private func formatAsGemmaTurn(_ prompt: String) -> String {
        "<|turn>user\n\(prompt)\n<turn|>\n<|turn>model\n"
    }
}

extension LiteRtLmBridge: NativeLiteRtLmEngine {
    func generate(
        prompt: String,
        modelPath: String,
        maxTokens: Int32,
        completion: @escaping (NativeLiteRtLmResult) -> Void
    ) {
        Task {
            let start = Date()
            do {
                let engine = try await ensureEngine(modelPath: modelPath)
                let text = try await engine.generate(
                    prompt: formatAsGemmaTurn(prompt),
                    temperature: 0.7,
                    maxTokens: Int(maxTokens)
                )
                let elapsedMs = Int64((Date().timeIntervalSince(start) * 1000).rounded())
                let approxTokens = max(1, text.split(separator: " ").count)
                completion(
                    NativeLiteRtLmResult(
                        text: text,
                        errorMessage: nil,
                        latencyMillis: elapsedMs,
                        tokensGenerated: Int32(approxTokens)
                    )
                )
            } catch {
                completion(
                    NativeLiteRtLmResult(
                        text: nil,
                        errorMessage: "\(error)",
                        latencyMillis: 0,
                        tokensGenerated: 0
                    )
                )
            }
        }
    }
}
