# Sprint 3 — Task 02

# Add Local Model and Validate Real LiteRT Inference

## Role

You are a Senior Kotlin Multiplatform Engineer and Edge AI Engineer working on ASTRA.

## References

Before implementing this task, read:

* README.md
* ROADMAP.md
* ENGINEERING_GUIDE.md
* docs/05_Edge_AI_Runtime_Evaluation.md
* existing LiteRT implementation from Sprint 3 Task 01

---

# Context

Sprint 3 Task 01 introduced:

* LiteRT dependency
* `LiteRtInferenceEngine`
* `LocalModelLoader`
* `EdgeRuntimeSession`
* Android `org.tensorflow.lite.Interpreter`
* automatic fallback to `MockInferenceEngine`
* iOS Mock fallback
* platform-aware `BackendCatalog`

However, no real `.tflite` model is currently available in the repository.

Therefore, ASTRA cannot yet prove a full real local inference.

---

# Goal

Add a compatible local `.tflite` model and validate that ASTRA can execute a real LiteRT inference end-to-end on Android.

---

# Scope

Implement only the model integration and validation path.

---

## 1. Model Placement

Add or document the expected model location:

```text
shared/src/androidMain/assets/models/astra-slm.tflite
```

If the model file is too large for Git, document how to place it locally.

---

## 2. Model Compatibility

Verify that the selected model is compatible with the current `LiteRtInferenceEngine`.

Check:

* input tensor shape
* input tensor type
* output tensor shape
* output tensor type
* tokenizer requirements if any

If the current engine is too generic for the selected model, adapt it minimally.

---

## 3. First Real Inference

When the model exists:

* load `astra-slm.tflite`;
* run `Interpreter.run(...)`;
* return a real `GenerationResult`;
* display runtime as `LiteRT`;
* display mode as `Real Local Inference`.

---

## 4. Fallback Preservation

If the model is missing or incompatible:

* do not crash;
* fall back to `MockInferenceEngine`;
* display fallback status clearly.

---

## 5. Metrics

Capture and display:

* model load time
* inference latency
* total execution time
* backend name
* model name
* runtime mode: Real / Fallback

Do not fake tokens/sec if unavailable.

---

## 6. Documentation

Update or create:

```text
docs/REAL_INFERENCE_SETUP.md
```

Include:

* where to place the model;
* expected filename;
* model compatibility notes;
* how to verify real inference;
* known limitations.

---

# Out of Scope

Do NOT implement:

* model download manager
* multiple real models
* streaming
* tokenizer-heavy SLM support unless required
* ONNX Runtime
* Core ML
* benchmark real execution
* iOS real inference

---

# Acceptance Criteria

The task is complete only if:

* A compatible `.tflite` model path is supported.
* Android runs a real `Interpreter.run(...)` when the model exists.
* Assistant clearly shows Real LiteRT mode.
* Missing model falls back to Mock.
* Runtime errors are handled safely.
* Metrics are displayed.
* Setup documentation exists.
* Android builds.
* iOS builds.
* UI remains decoupled from LiteRT.

---

# Commit Message

```text
feat: validate real LiteRT inference
```
