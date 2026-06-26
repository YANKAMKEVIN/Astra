# Sprint 2 — Task 03

# Device Capabilities

## Role

You are a Senior Kotlin Multiplatform Engineer working on the ASTRA project.

## References

Read the project documentation before implementation.

---

# Goal

Introduce ASTRA’s Device Capability layer.

The application must expose platform capabilities through a clean abstraction so that Android and iOS can provide their own implementation.

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

The Dashboard currently displays placeholder capability data.

This task must replace placeholders with a clean capability architecture.

---

# Scope

Implement only device capability detection and display.

---

## Create

### DeviceCapabilityProvider

Create a platform-agnostic interface:

```kotlin
interface DeviceCapabilityProvider {
    suspend fun getCapabilities(): DeviceCapabilities
}
```

---

### DeviceCapabilities

Suggested fields:

* platform
* osVersion
* deviceModel
* cpuName
* gpuName
* npuAvailable
* npuName
* totalMemoryMb
* availableMemoryMb
* storageAvailableGb
* supportedBackends
* supportedFeatures

---

### SupportedFeature

Supported values:

* LocalAI
* DocumentQA
* Benchmark
* NPU
* GPU
* OfflineMode

---

## Android Implementation

Create Android actual implementation using best-effort detection.

If some values are not available, return a safe fallback:

```text
Unknown
```

or

```text
Not detected
```

Do not over-engineer hardware detection.

---

## iOS Implementation

Create iOS actual implementation using best-effort detection.

If some values are not available, return safe fallback values.

---

## Dashboard Integration

Refactor Dashboard so that it displays data from `DeviceCapabilityProvider`.

Dashboard must display:

* platform
* OS version
* device model
* memory
* NPU status
* supported backends
* supported features

---

## Koin Integration

Register platform-specific `DeviceCapabilityProvider`.

The Dashboard ViewModel must receive it through dependency injection.

---

# Out of Scope

Do NOT implement:

* real NPU benchmark
* LiteRT
* ONNX Runtime
* Core ML
* performance profiling
* battery monitoring
* thermal monitoring

---

# Acceptance Criteria

The task is complete only if:

* `DeviceCapabilityProvider` exists.
* Android implementation exists.
* iOS implementation exists.
* Dashboard consumes real provider data.
* Dashboard no longer relies only on hardcoded placeholders.
* Safe fallback values are used when data is unavailable.
* Android builds.
* iOS builds.
* Domain/common code remains platform-agnostic.

---

# Expected Deliverables

* Device capability abstraction.
* Android capability provider.
* iOS capability provider.
* Dashboard integration.
* Clean Koin registration.

---

# Commit Message

```text
feat: add device capability provider
```
