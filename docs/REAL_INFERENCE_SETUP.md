# Real Inference Setup

ASTRA supports two Android real-inference validation paths:

1. LiteRT tensor validation through a plain `.tflite` model.
2. LiteRT-LM generative validation through a MediaPipe GenAI/LiteRT-LM bundle.

iOS continues to compile and run through the Mock fallback in v1.0.1.

## Chosen v1.0.1 model

Preferred model family:

```text
Gemma LiteRT-LM / MediaPipe LLM Inference compatible bundle
```

Recommended developer artifact:

```text
Gemma 3 1B / smallest available Gemma IT bundle for Android LLM Inference
```

If a `Gemma 4 E2B IT LiteRT-LM` bundle is available in your internal model source, use that. If it is too large for the target device, use the smallest compatible Gemma `.task` or `.litertlm` bundle available.

ASTRA does not commit model binaries to Git.

## LiteRT-LM generative model path

Place one compatible bundle here:

```text
shared/src/androidMain/assets/models/litert-lm/gemma.task
```

or:

```text
shared/src/androidMain/assets/models/litert-lm/gemma.litertlm
```

The Android runtime also tolerates a legacy split layout:

```text
shared/src/androidMain/assets/models/litert-lm/model.tflite
shared/src/androidMain/assets/models/litert-lm/tokenizer.model
```

For v1.0.1, the single `.task` or `.litertlm` bundle is the preferred setup because `com.google.mediapipe:tasks-genai` expects a model file path that contains the metadata required by `LlmInference`.

## Dependency

Android uses:

```text
com.google.mediapipe:tasks-genai:0.10.35
```

This dependency is Android-only. Shared common code depends only on ASTRA abstractions, so iOS remains decoupled from LiteRT-LM classes.

## LiteRT tensor validation path

ASTRA still supports the earlier tensor validation model at:

```text
shared/src/androidMain/assets/models/astra-slm.tflite
```

This path validates `Interpreter.run(...)` and numeric tensors. It is not a text-generation SLM path.

## Device requirements

Recommended:

* Android physical device;
* arm64-v8a support;
* at least 6 GB RAM for small Gemma-class bundles;
* enough free storage for the model bundle;
* thermal headroom for local generation.

Emulators may work for smoke tests but are not recommended for performance validation.

## How to verify LiteRT-LM real inference

1. Add the model bundle:

   ```text
   shared/src/androidMain/assets/models/litert-lm/gemma.task
   ```

2. Build Android:

   ```bash
   ./gradlew :androidApp:assembleDebug --no-configuration-cache
   ```

3. Open ASTRA Settings.

4. Select:

   ```text
   Model: Gemma 3 1B
   Backend: LiteRT-LM
   ```

5. Open Assistant and ask a question.

6. Confirm the metrics card shows:

   ```text
   LiteRT-LM Generative Runtime
   ```

7. Confirm the response body is generated model text and does not show a fallback reason.

## Fallback behavior

If the bundle is missing, invalid, too large, unsupported by the device or fails initialization:

* ASTRA does not crash;
* `MockInferenceEngine` is used automatically;
* the Assistant metrics card displays the fallback reason;
* Model Manager shows the missing bundle path;
* iOS remains on Mock fallback.

## Known limitations

* No model download manager.
* No Hugging Face integration.
* No cloud fallback.
* No streaming UI.
* No iOS real inference yet.
* Only one real LiteRT-LM bundle is supported for v1.0.1.
* Tokens/sec is computed from generated token count when `LlmInference.sizeInTokens(...)` is available; otherwise ASTRA falls back to a word-count approximation.
