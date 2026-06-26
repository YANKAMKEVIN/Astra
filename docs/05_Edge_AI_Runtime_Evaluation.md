# Edge AI Runtime Evaluation

## Sprint 3 decision

ASTRA starts real Edge AI integration with **LiteRT on Android**.

## Options evaluated

| Runtime | Fit for ASTRA | Notes |
|---|---|---|
| LiteRT | Best Sprint 3 fit | Android-first, local execution, Google AI Edge roadmap, natural fit for TFLite/LiteRT assets and future accelerator delegates. |
| ONNX Runtime | Strong future candidate | Portable and mature, but heavier for the first Android-only runtime slice. Kept in the catalog for Sprint 4-style expansion. |
| llama.cpp | Useful for GGUF experiments | Good SLM ecosystem fit, but native packaging/lifecycle work is larger than the first runtime integration. |
| MLX | Future iOS research | Interesting for Apple silicon, but not an Android runtime and not the right first integration point for the KMP mobile MVP. |

## Selected approach

The Sprint 3 implementation introduces a replaceable LiteRT runtime path behind the existing `InferenceEngine` abstraction:

```text
Assistant
  ↓
AskLocalAssistantUseCase
  ↓
InferenceEngine
  ↓
LiteRtInferenceEngine
  ↓
LocalModelLoader + EdgeRuntimeSession
```

Android exposes LiteRT as an installed backend. iOS remains on Mock fallback until the Core ML runtime is implemented.

## Local model policy

ASTRA does not download models in Sprint 3. To enable the real runtime path, place a compatible local model at:

```text
shared/src/androidMain/assets/models/astra-slm.tflite
```

If the asset is missing, invalid, or the runtime cannot initialize, ASTRA automatically falls back to `MockInferenceEngine` and keeps the app functional.

## Fallback behavior

Fallback is intentional and user-safe:

* runtime/model failures are logged;
* Mock inference remains available;
* the generated answer explicitly states that fallback is active;
* the application does not crash when no model is bundled.

