# Sprint 2 — Task 01

# AI Model Catalog

## Role

You are a Senior Kotlin Multiplatform Engineer working on the ASTRA project.

## References

Read the project documentation before implementation.

---

# Goal

Introduce a centralized AI Model Catalog responsible for exposing every model available inside ASTRA.

The application must no longer manipulate model names directly.

Every feature must rely on a single source of truth.

---

# Scope

Implement only the model catalog.

---

## Create

### LocalModel

Represent one AI model.

Suggested fields:

* id
* displayName
* provider
* parameterCount
* quantization
* contextWindow
* supportedBackends
* minimumMemoryMb
* status

---

### ModelStatus

Supported values:

* Installed
* Available
* DownloadRequired
* Unsupported

---

### ModelProvider

Supported providers:

* Google
* Microsoft
* Meta
* Alibaba
* Mistral AI
* Mock

---

### ModelCatalog

Expose:

* availableModels()
* installedModels()
* currentModel()
* selectModel()
* modelById()

The catalog must be platform independent.

---

## Default Models

Register:

* Mock Model
* Gemma 3 1B
* Phi-3 Mini
* Llama 3.2 3B
* Qwen 2.5 1.5B

Only Mock Model is Installed.

Others are Available.

---

## Koin

Register the catalog.

---

## Assistant

Assistant must retrieve the selected model from the catalog.

---

## Settings

Settings must display models coming from the catalog instead of hardcoded values.

---

## Benchmark

Benchmark must also consume the catalog.

---

# Out of Scope

Do NOT implement:

* model download
* GGUF
* LiteRT
* ONNX
* CoreML
* persistence

---

# Acceptance Criteria

* No hardcoded model list remains.
* Settings uses ModelCatalog.
* Benchmark uses ModelCatalog.
* Assistant uses currentModel().
* Android builds.
* iOS builds.

---

# Commit Message

```text
feat: introduce AI model catalog
```
