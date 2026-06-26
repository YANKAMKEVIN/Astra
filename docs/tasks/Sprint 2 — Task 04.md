# Sprint 2 — Task 04

# Inference Backend Catalog

## Role

You are a Senior Kotlin Multiplatform Engineer working on the ASTRA project.

## References

Read the project documentation before implementation.

---

# Goal

Introduce a centralized catalog for inference backends.

The application must no longer manipulate backend names directly.

Every feature must rely on a single source of truth for available inference engines.

---

# Context

ASTRA already contains:

* Assistant
* Documents Assistant
* Benchmark Lab
* Settings
* MockInferenceEngine
* ModelCatalog
* PromptPipeline
* DeviceCapabilityProvider

The project now needs a clean abstraction for inference backends before integrating real engines such as LiteRT, ONNX Runtime and Core ML.

---

# Scope

Implement only the backend catalog.

---

## Create

### InferenceBackendInfo

Represents one inference backend.

Suggested fields:

* id
* displayName
* provider
* supportedPlatforms
* supportedModelFormats
* accelerationTargets
* status
* description

---

### BackendStatus

Supported values:

* Installed
* Available
* ComingSoon
* Unsupported

---

### BackendProvider

Supported values:

* ASTRA
* Google
* Microsoft
* Apple
* GGML
* Qualcomm
* Mock

---

### AccelerationTarget

Supported values:

* CPU
* GPU
* NPU
* ANE
* Metal
* NNAPI

---

### BackendCatalog

Expose:

* availableBackends()
* installedBackends()
* currentBackend()
* selectBackend()
* backendById()

The catalog must be platform independent.

---

## Default Backends

Register:

* Mock Engine
* LiteRT
* ONNX Runtime
* Core ML
* llama.cpp

Only Mock Engine is Installed.

Other backends must be Available, ComingSoon or Unsupported depending on platform if the project already supports platform checks.

---

## Koin

Register the catalog.

---

## Settings Integration

Settings must display backends from `BackendCatalog`.

Do not hardcode backend names in the Settings screen.

---

## Assistant Integration

Assistant must use `BackendCatalog.currentBackend()` when building inference requests.

---

## Benchmark Integration

Benchmark must consume backend information from `BackendCatalog`.

---

# Out of Scope

Do NOT implement:

* LiteRT
* ONNX Runtime
* Core ML
* llama.cpp
* real backend loading
* native bindings
* performance benchmarks
* persistence

---

# Acceptance Criteria

The task is complete only if:

* `BackendCatalog` exists.
* `InferenceBackendInfo` exists.
* Settings uses `BackendCatalog`.
* Assistant uses the current backend from `BackendCatalog`.
* Benchmark uses backend data from `BackendCatalog`.
* No hardcoded backend list remains in UI.
* Android builds.
* iOS builds.
* No real inference backend is introduced.

---

# Expected Deliverables

* Centralized backend catalog.
* Clean backend metadata model.
* Settings refactored to consume backend catalog.
* Assistant and Benchmark aligned with backend catalog.

---

# Commit Message

```text
feat: introduce inference backend catalog
```
