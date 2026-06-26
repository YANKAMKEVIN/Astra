# Sprint 3 — Task 01

# Real Edge AI Integration

## Role

You are a Senior Kotlin Multiplatform Engineer and Edge AI Engineer working on the ASTRA project.

## References

Before implementing this task, read:

* README.md
* ROADMAP.md
* ENGINEERING_GUIDE.md
* docs/02_Functional_Requirements.md
* docs/03_Platform_Architecture.md
* docs/04_Design_System.md

These documents are the source of truth.

---

# Context

ASTRA already contains:

* Clean Architecture
* MVI
* Koin
* Prompt Pipeline
* Assistant
* Documents Assistant
* Benchmark Lab
* Settings
* Model Catalog
* Backend Catalog
* Device Capability Provider
* MockInferenceEngine
* Persistent AI Configuration

The application architecture is complete.

The next objective is to replace the mocked inference with a **real on-device inference engine**.

This is the primary objective of Sprint 3.

---

# Goal

Integrate the first real Edge AI inference engine compatible with the ASTRA architecture.

The application must execute a real Small Language Model (SLM) locally on the device without relying on cloud inference.

The selected solution should be the most appropriate considering:

* Android compatibility
* Kotlin integration
* Local execution
* Ease of integration
* Future extensibility

LiteRT is the preferred candidate, but if another solution is demonstrably more appropriate, document the rationale before implementation.

---

# Scope

Implement only the first real inference engine integration.

---

## 1. Technology Evaluation

Briefly evaluate the available options for ASTRA:

* LiteRT
* ONNX Runtime
* llama.cpp
* MLX (if relevant for future iOS support)

Document why the selected solution is the best fit for Sprint 3.

The evaluation can remain concise and be added to the project documentation if appropriate.

---

## 2. Real Inference Engine

Create a new implementation of:

```kotlin
InferenceEngine
```

Suggested name:

```kotlin
LiteRtInferenceEngine
```

or another appropriate name depending on the chosen technology.

The implementation must remain fully compatible with the existing architecture.

---

## 3. Model Loading

Introduce a dedicated model loader.

Responsibilities include:

* loading the local model;
* validating model availability;
* exposing loading status;
* reporting meaningful errors.

The model loader must be independent from UI components.

---

## 4. Runtime Session

Create a runtime/session component responsible for:

* initializing the inference runtime;
* managing lifecycle;
* releasing resources when appropriate.

The inference engine must delegate runtime management to this component.

---

## 5. Backend Integration

Integrate the new backend with:

* BackendCatalog
* Settings
* DeviceCapabilityProvider

The application should expose whether the backend is:

* Installed
* Available
* Unsupported

based on the current platform.

---

## 6. Assistant Integration

Update the Assistant flow:

Assistant

↓

AskLocalAssistantUseCase

↓

PromptPipeline

↓

InferenceEngine

↓

Real Local Model

No UI architecture changes should be required.

---

## 7. Fallback Strategy

If the selected backend cannot be initialized:

* automatically fall back to `MockInferenceEngine`;
* display an appropriate message to the engineer;
* keep the application fully functional.

The application must never crash because of an unavailable local model.

---

## 8. Logging

Provide useful logs for:

* runtime initialization;
* model loading;
* inference execution;
* runtime failures;
* fallback activation.

Logs should help diagnose Edge AI integration issues.

---

# Out of Scope

Do NOT implement:

* model download manager;
* streaming generation;
* multimodal inference;
* OCR;
* speech recognition;
* cloud fallback;
* benchmark measurements;
* RAG improvements.

These will be addressed in later tasks.

---

# Acceptance Criteria

The task is complete only if:

* A real Edge AI runtime is integrated.
* `InferenceEngine` has a real implementation.
* The application can initialize the selected runtime.
* The application can load a local model.
* Assistant is capable of executing a real local inference when available.
* Mock engine remains available as fallback.
* BackendCatalog reflects the runtime status.
* Android builds successfully.
* iOS continues to build successfully without regression.
* Existing architecture remains unchanged.

---

# Expected Deliverables

* First real Edge AI runtime integrated.
* Local model loading.
* Runtime abstraction.
* Graceful fallback mechanism.
* Architecture preserved.

---

# Definition of Success

Sprint 3 is considered successfully started when ASTRA performs its first real local inference without relying on any cloud service.

---

# Commit Message

```text
feat: integrate first real edge ai runtime
```
