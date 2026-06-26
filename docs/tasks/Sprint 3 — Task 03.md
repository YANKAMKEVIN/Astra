# Sprint 3 — Task 03

# LiteRT-LM Generative Runtime Evaluation

## Role

You are a Senior Kotlin Multiplatform Engineer and Edge AI Engineer working on ASTRA.

## References

Before implementing this task, read:

* README.md
* ROADMAP.md
* ENGINEERING_GUIDE.md
* docs/05_Edge_AI_Runtime_Evaluation.md
* docs/REAL_INFERENCE_SETUP.md
* existing LiteRT runtime implementation

---

# Context

ASTRA currently supports:

* LiteRT Android runtime integration
* `org.tensorflow.lite.Interpreter.run(...)`
* local `.tflite` model loading path
* Mock fallback
* real/fallback runtime display
* runtime metrics

However, a simple LiteRT `Interpreter.run(...)` is not enough to prove full SLM text generation.

A real SLM requires:

* tokenizer
* prompt encoding
* token generation loop
* output decoding
* runtime session management
* model-specific configuration

LiteRT-LM is the preferred candidate for this layer.

---

# Goal

Evaluate and prepare ASTRA for real generative SLM inference using LiteRT-LM.

The goal is not necessarily to complete full model integration in this task.

The goal is to determine the cleanest way to integrate LiteRT-LM into ASTRA without breaking the current architecture.

---

# Scope

Implement only the LiteRT-LM evaluation and integration foundation.

---

## 1. Technical Evaluation

Evaluate LiteRT-LM for ASTRA.

Document:

* required dependencies;
* supported Android versions;
* supported model formats;
* recommended model candidates;
* tokenizer requirements;
* expected model size;
* integration complexity;
* limitations.

Update or create:

```text
docs/06_LiteRT_LM_Evaluation.md
```

---

## 2. Architecture Decision

Create an ADR:

```text
docs/adr/ADR-001-LiteRT-vs-LiteRT-LM.md
```

The ADR must explain:

* why LiteRT `Interpreter.run(...)` is useful but insufficient for SLM generation;
* why LiteRT-LM is more appropriate for text generation;
* how ASTRA will keep LiteRT simple runtime as a low-level backend;
* how LiteRT-LM will be integrated behind `InferenceEngine`.

---

## 3. LiteRT-LM Engine Skeleton

Create a new engine skeleton:

```kotlin
LiteRtLmInferenceEngine
```

It must implement:

```kotlin
InferenceEngine
```

For now, it may return a controlled fallback result if no compatible LiteRT-LM model is available.

The important point is to prepare the architecture cleanly.

---

## 4. LiteRT-LM Model Loader

Create:

```kotlin
LiteRtLmModelLoader
```

Responsibilities:

* locate model files;
* validate expected files;
* expose model availability;
* return clear error states.

Expected model folder:

```text
shared/src/androidMain/assets/models/litert-lm/
```

Do not commit a large model binary unless explicitly available and appropriate.

---

## 5. Runtime Status

Extend runtime status to distinguish:

* Mock Fallback
* LiteRT Tensor Runtime
* LiteRT-LM Generative Runtime
* Model Missing
* Unsupported Platform

The Assistant must be able to display these statuses.

---

## 6. Backend Catalog

Update BackendCatalog so that Android can expose:

* Mock Engine: Installed
* LiteRT: Installed
* LiteRT-LM: Available or ModelRequired

iOS should remain stable:

* Mock Engine: Installed
* LiteRT-LM: Unsupported or Future
* Core ML: Available / Future

---

# Out of Scope

Do NOT implement:

* full Gemma generation;
* model download manager;
* tokenizer implementation from scratch;
* streaming output;
* benchmark real execution;
* iOS Core ML;
* ONNX Runtime;
* cloud fallback.

---

# Acceptance Criteria

The task is complete only if:

* LiteRT-LM evaluation documentation exists.
* ADR comparing LiteRT and LiteRT-LM exists.
* `LiteRtLmInferenceEngine` skeleton exists.
* `LiteRtLmModelLoader` exists.
* BackendCatalog exposes LiteRT-LM clearly.
* Assistant can display LiteRT-LM runtime status.
* Mock fallback remains stable.
* Android builds.
* iOS builds.
* No architecture violation is introduced.

---

# Expected Deliverables

* Clear LiteRT-LM technical evaluation.
* Architecture decision record.
* LiteRT-LM engine skeleton.
* LiteRT-LM model loading foundation.
* Runtime status improvements.

---

# Commit Message

```text
feat: prepare LiteRT-LM generative runtime
```
