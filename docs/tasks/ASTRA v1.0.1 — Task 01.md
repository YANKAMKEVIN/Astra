# ASTRA v1.0.1 — Task 01

# Integrate Real LiteRT-LM Model

## Role

You are a Senior Kotlin Multiplatform Engineer and Edge AI Engineer working on ASTRA.

## References

Before implementing this task, read:

* README.md
* ROADMAP.md
* ENGINEERING_GUIDE.md
* docs/06_LiteRT_LM_Evaluation.md
* docs/REAL_INFERENCE_SETUP.md
* docs/adr/ADR-001-LiteRT-vs-LiteRT-LM.md
* existing LiteRT-LM foundation

---

# Context

ASTRA v1.0.0 is complete.

The app already includes:

* MockInferenceEngine
* LiteRT Tensor Runtime foundation
* LiteRT-LM foundation
* RoutingInferenceEngine
* Model Manager
* BackendCatalog
* runtime status display
* fallback handling

However, ASTRA does not yet execute a real generative SLM.

A simple LiteRT `Interpreter.run(...)` validates tensor runtime integration but is not sufficient for full text generation.

The goal of v1.0.1 is to validate a real local generative model using LiteRT-LM.

---

# Goal

Integrate one real LiteRT-LM compatible model and execute a real local SLM inference on Android.

The result must be visible in the Assistant screen.

---

# Preferred Model

Use a small LiteRT-LM compatible model.

Preferred candidate:

```text
Gemma 4 E2B IT LiteRT-LM
```

If that model is too large or impractical, choose the smallest available LiteRT-LM compatible Gemma model.

Document the choice.

---

# Scope

Implement only one real model integration.

---

## 1. Model Acquisition Strategy

Do not commit large model binaries if they are too large for Git.

Instead, support a local developer setup path:

```text
shared/src/androidMain/assets/models/litert-lm/
```

Document exactly which files are required.

If LiteRT-LM uses a single `.litertlm` bundle, document that file path clearly.

Example:

```text
shared/src/androidMain/assets/models/litert-lm/gemma.litertlm
```

---

## 2. LiteRT-LM Dependency Integration

Add the required LiteRT-LM Android dependency if not already present.

Keep it Android-only.

iOS must continue to compile with fallback.

---

## 3. LiteRtLmInferenceEngine Implementation

Replace the current skeleton with a real implementation.

It must:

* load the LiteRT-LM model bundle;
* initialize the LiteRT-LM session;
* send a prompt;
* generate text;
* return `GenerationResult`;
* include runtime metrics.

---

## 4. Metrics

Capture at least:

* model load time;
* first response latency if available;
* generation latency;
* total execution time;
* generated text length;
* backend name;
* model name;
* runtime mode.

If tokens/sec can be computed reliably, compute it.

If not, display `N/A`.

---

## 5. Assistant Integration

When LiteRT-LM is selected and model files exist:

* Assistant must use the real LiteRT-LM runtime;
* runtime mode must display:

```text
LiteRT-LM Generative Runtime
```

* response must be real generated text.

When model files are missing:

* fallback to Mock;
* display missing file reason.

---

## 6. Model Manager Integration

Update Model Manager so it can show:

* model installed;
* model missing;
* expected path;
* expected size if known;
* setup guide reference.

---

## 7. Documentation

Update:

```text
docs/REAL_INFERENCE_SETUP.md
```

Add:

* chosen model;
* download/source instructions;
* expected local file path;
* device requirements;
* known limitations;
* how to verify that real inference is running.

---

# Out of Scope

Do NOT implement:

* multiple real models;
* model download manager;
* iOS real runtime;
* Core ML;
* ONNX Runtime;
* streaming UI;
* multi-modal input;
* benchmark history;
* cloud fallback.

---

# Acceptance Criteria

The task is complete only if:

* A real LiteRT-LM model path is supported.
* Android can run a real generative inference when model files exist.
* Assistant displays real generated text.
* Runtime mode clearly says `LiteRT-LM Generative Runtime`.
* Missing model files fall back safely to Mock.
* Model Manager explains model status.
* Documentation explains setup clearly.
* Android builds.
* iOS builds.
* UI remains decoupled from LiteRT-LM classes.

---

# Expected Deliverables

* Real LiteRT-LM model integration.
* Real local SLM inference on Android.
* Updated Model Manager.
* Updated setup documentation.
* Stable fallback path.

---

# Commit Message

```text
feat: validate real LiteRT-LM inference
```
