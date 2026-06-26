# Sprint 1 — Task 03

# Settings Screen

## Role

You are a Senior Kotlin Multiplatform Engineer working on the ASTRA project.

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

ASTRA already has:

* Navigation
* Design System
* MVI foundation
* Koin
* Assistant screen
* `InferenceEngine`
* `MockInferenceEngine`
* `AskLocalAssistantUseCase`

The Assistant can now generate mocked responses through the AI abstraction layer.

---

# Goal

Implement a functional Settings screen allowing the engineer to configure the local AI environment.

The screen must prepare ASTRA for future model and backend switching.

---

# Scope

Implement only Settings UI and in-memory configuration.

---

## 1. Settings State

Create a Settings state containing:

* selectedModel
* selectedBackend
* selectedIndustry
* temperature
* maxTokens
* contextWindow
* quantization
* experimentalFeaturesEnabled

---

## 2. Supported Models

Display selectable models:

* Mock Model
* Gemma
* Phi
* Llama
* Qwen

Only `Mock Model` is functional for now.

Other models must be displayed as unavailable or coming soon.

---

## 3. Supported Backends

Display selectable backends:

* Mock Engine
* LiteRT
* ONNX Runtime
* Core ML
* llama.cpp

Only `Mock Engine` is functional for now.

Other backends must be displayed as unavailable or coming soon.

---

## 4. Inference Parameters

Allow the engineer to configure:

* Temperature
* Max tokens
* Context window
* Quantization

Suggested defaults:

* Temperature: 0.3
* Max tokens: 512
* Context window: 4096
* Quantization: 4-bit

---

## 5. UI Layout

The screen must use premium ASTRA sections:

* Model Configuration
* Backend Configuration
* Inference Parameters
* Experimental Features

Use:

* AstraCard
* AstraChip
* AstraButton
* AstraMetricCard if useful

---

## 6. Behaviour

The engineer must be able to:

* select the functional model;
* select the functional backend;
* view unavailable models/backends;
* change inference parameters;
* toggle experimental features.

For now, configuration can remain in-memory.

No persistence is required in this task.

---

## 7. Integration with Assistant

The Assistant must read the selected configuration if a shared configuration holder already exists.

If no shared configuration holder exists yet, create a simple in-memory `AiConfigurationRepository`.

It must expose:

* current configuration
* update model
* update backend
* update temperature
* update max tokens
* update context window
* update quantization

The Assistant ViewModel must use this configuration when building `PromptRequest`.

---

# Out of Scope

Do NOT implement:

* persistent storage
* real model download
* real backend switching
* LiteRT
* ONNX Runtime
* Core ML
* llama.cpp
* benchmark
* documents
* cloud API

---

# Acceptance Criteria

The task is complete only if:

* Settings screen is reachable.
* Model list is displayed.
* Backend list is displayed.
* Only Mock Model and Mock Engine are selectable.
* Other models/backends are visible but disabled or marked as coming soon.
* Inference parameters can be changed.
* Experimental features toggle works.
* Assistant uses the selected configuration where applicable.
* Android builds.
* iOS builds.
* No real AI inference is introduced.
* No persistence is introduced.

---

# Expected Deliverables

* Functional Settings screen.
* In-memory AI configuration.
* Assistant connected to configuration.
* Clean MVI implementation.

---

# Commit Message

```text
feat: implement AI settings
```
