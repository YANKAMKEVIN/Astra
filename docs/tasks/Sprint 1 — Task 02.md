# Sprint 1 — Task 02

# Mock Inference Engine

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

The Assistant screen has already been implemented.

It currently displays:

* industry selector
* question input
* mocked generation
* response card
* metrics panel

The current mock response may be implemented directly inside the Assistant ViewModel or screen logic.

This task must move the mock generation logic into a proper AI abstraction layer.

---

# Goal

Introduce ASTRA’s first AI engine abstraction.

The Assistant feature must no longer own generation logic directly.

Instead, it must call a domain/use-case layer that delegates generation to a `MockInferenceEngine`.

This prepares the project for future LiteRT, ONNX Runtime and Core ML integrations.

---

# Scope

Implement only the mock inference architecture.

---

## 1. Core AI Models

Create domain/core models representing an inference request and result.

At minimum:

* `PromptRequest`
* `GenerationResult`
* `GenerationMetrics`
* `AiModel`
* `InferenceBackend`

Suggested fields:

### PromptRequest

* prompt
* industry
* model
* backend
* maxTokens
* temperature

### GenerationResult

* text
* metrics
* model
* backend
* generatedAt

### GenerationMetrics

* latencyMillis
* timeToFirstTokenMillis
* tokensGenerated
* tokensPerSecond
* memoryUsageMb

### AiModel

Examples:

* Mock Model
* Gemma
* Phi
* Llama
* Qwen

### InferenceBackend

Examples:

* Mock Engine
* LiteRT
* ONNX Runtime
* Core ML
* llama.cpp

---

## 2. InferenceEngine Interface

Create an interface:

```kotlin
interface InferenceEngine {
    suspend fun generate(request: PromptRequest): GenerationResult
}
```

The interface must be platform-agnostic.

It must not depend on Android or iOS.

---

## 3. MockInferenceEngine

Create `MockInferenceEngine`.

It must:

* implement `InferenceEngine`;
* simulate a short delay;
* return a realistic answer based on the selected industry;
* return realistic metrics;
* remain fully offline;
* require no network;
* require no real model file.

Suggested responses:

* Industrial Maintenance: pump restart procedure
* Aerospace: cockpit checklist assistance
* Defense: secure offline procedure assistance
* Energy: site incident diagnosis
* Healthcare: medical device troubleshooting

---

## 4. Use Case

Create a use case:

```kotlin
class AskLocalAssistantUseCase(
    private val inferenceEngine: InferenceEngine
)
```

It must expose:

```kotlin
suspend operator fun invoke(request: PromptRequest): GenerationResult
```

No UI logic inside the use case.

---

## 5. Koin Integration

Register:

* `InferenceEngine` as `MockInferenceEngine`
* `AskLocalAssistantUseCase`

The Assistant ViewModel must receive the use case through dependency injection.

---

## 6. Assistant Refactor

Refactor the Assistant feature so that:

* the ViewModel builds a `PromptRequest`;
* the ViewModel calls `AskLocalAssistantUseCase`;
* the use case calls `InferenceEngine`;
* the response and metrics come from `GenerationResult`.

The Assistant screen must not know about `MockInferenceEngine`.

---

# Out of Scope

Do NOT implement:

* LiteRT
* ONNX Runtime
* Core ML
* real model loading
* document RAG
* benchmark execution
* persistence
* network
* cloud API

---

# Acceptance Criteria

The task is complete only if:

* `InferenceEngine` exists.
* `MockInferenceEngine` exists.
* `AskLocalAssistantUseCase` exists.
* Koin provides the mock engine and use case.
* Assistant ViewModel uses the use case.
* Assistant screen still behaves exactly as before.
* Responses vary depending on selected industry.
* Metrics come from `GenerationResult`.
* Android builds.
* iOS builds.
* No platform-specific dependency leaks into domain/core AI models.

---

# Expected Deliverables

* AI abstraction foundation.
* Mock inference implementation.
* Assistant refactored to use the AI engine architecture.
* No visible regression in the Assistant screen.

---

# Commit Message

```text
feat: add mock inference engine
```
# Sprint 1 — Task 02

# Mock Inference Engine

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

The Assistant screen has already been implemented.

It currently displays:

* industry selector
* question input
* mocked generation
* response card
* metrics panel

The current mock response may be implemented directly inside the Assistant ViewModel or screen logic.

This task must move the mock generation logic into a proper AI abstraction layer.

---

# Goal

Introduce ASTRA’s first AI engine abstraction.

The Assistant feature must no longer own generation logic directly.

Instead, it must call a domain/use-case layer that delegates generation to a `MockInferenceEngine`.

This prepares the project for future LiteRT, ONNX Runtime and Core ML integrations.

---

# Scope

Implement only the mock inference architecture.

---

## 1. Core AI Models

Create domain/core models representing an inference request and result.

At minimum:

* `PromptRequest`
* `GenerationResult`
* `GenerationMetrics`
* `AiModel`
* `InferenceBackend`

Suggested fields:

### PromptRequest

* prompt
* industry
* model
* backend
* maxTokens
* temperature

### GenerationResult

* text
* metrics
* model
* backend
* generatedAt

### GenerationMetrics

* latencyMillis
* timeToFirstTokenMillis
* tokensGenerated
* tokensPerSecond
* memoryUsageMb

### AiModel

Examples:

* Mock Model
* Gemma
* Phi
* Llama
* Qwen

### InferenceBackend

Examples:

* Mock Engine
* LiteRT
* ONNX Runtime
* Core ML
* llama.cpp

---

## 2. InferenceEngine Interface

Create an interface:

```kotlin
interface InferenceEngine {
    suspend fun generate(request: PromptRequest): GenerationResult
}
```

The interface must be platform-agnostic.

It must not depend on Android or iOS.

---

## 3. MockInferenceEngine

Create `MockInferenceEngine`.

It must:

* implement `InferenceEngine`;
* simulate a short delay;
* return a realistic answer based on the selected industry;
* return realistic metrics;
* remain fully offline;
* require no network;
* require no real model file.

Suggested responses:

* Industrial Maintenance: pump restart procedure
* Aerospace: cockpit checklist assistance
* Defense: secure offline procedure assistance
* Energy: site incident diagnosis
* Healthcare: medical device troubleshooting

---

## 4. Use Case

Create a use case:

```kotlin
class AskLocalAssistantUseCase(
    private val inferenceEngine: InferenceEngine
)
```

It must expose:

```kotlin
suspend operator fun invoke(request: PromptRequest): GenerationResult
```

No UI logic inside the use case.

---

## 5. Koin Integration

Register:

* `InferenceEngine` as `MockInferenceEngine`
* `AskLocalAssistantUseCase`

The Assistant ViewModel must receive the use case through dependency injection.

---

## 6. Assistant Refactor

Refactor the Assistant feature so that:

* the ViewModel builds a `PromptRequest`;
* the ViewModel calls `AskLocalAssistantUseCase`;
* the use case calls `InferenceEngine`;
* the response and metrics come from `GenerationResult`.

The Assistant screen must not know about `MockInferenceEngine`.

---

# Out of Scope

Do NOT implement:

* LiteRT
* ONNX Runtime
* Core ML
* real model loading
* document RAG
* benchmark execution
* persistence
* network
* cloud API

---

# Acceptance Criteria

The task is complete only if:

* `InferenceEngine` exists.
* `MockInferenceEngine` exists.
* `AskLocalAssistantUseCase` exists.
* Koin provides the mock engine and use case.
* Assistant ViewModel uses the use case.
* Assistant screen still behaves exactly as before.
* Responses vary depending on selected industry.
* Metrics come from `GenerationResult`.
* Android builds.
* iOS builds.
* No platform-specific dependency leaks into domain/core AI models.

---

# Expected Deliverables

* AI abstraction foundation.
* Mock inference implementation.
* Assistant refactored to use the AI engine architecture.
* No visible regression in the Assistant screen.

---

# Commit Message

```text
feat: add mock inference engine
```
