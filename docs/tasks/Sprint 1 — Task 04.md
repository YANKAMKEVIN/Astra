# Sprint 1 — Task 04

# Benchmark Lab

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
* Settings screen
* `InferenceEngine`
* `MockInferenceEngine`
* `AskLocalAssistantUseCase`
* in-memory AI configuration

The Assistant can generate mocked responses through the AI abstraction layer.

---

# Goal

Implement the first version of ASTRA’s Benchmark Lab.

The goal is to compare multiple AI models on the same prompt using mocked benchmark results.

This screen must demonstrate the future capability of ASTRA to evaluate Edge AI models on-device.

---

# Scope

Implement only mocked benchmark execution.

---

## 1. Benchmark State

Create a Benchmark state containing:

* prompt
* selectedModels
* selectedBackend
* isRunning
* results
* recommendedModel
* error

---

## 2. Benchmark Prompt

Provide a default prompt:

```text
How should an engineer restart Pump A after an emergency shutdown?
```

The engineer must be able to edit the prompt.

---

## 3. Model Selection

Display selectable models:

* Mock Model
* Gemma
* Phi
* Llama
* Qwen

For this task, all models can be selectable, but results must be mocked.

---

## 4. Backend Selection

Display backend:

* Mock Engine

Other backends may be displayed as coming soon if already available in the project.

---

## 5. Run Benchmark

When the engineer taps:

```text
Run Benchmark
```

ASTRA must:

* simulate benchmark execution;
* show loading state;
* generate mocked results for each selected model;
* compute a recommended model.

---

## 6. Benchmark Result

Each result must display:

* model
* backend
* latency
* time to first token
* tokens/sec
* memory usage
* quality score
* status

---

## 7. Recommendation

Compute a simple recommendation.

Example rule:

* prefer highest quality score;
* then highest tokens/sec;
* then lowest latency;
* then lowest memory usage.

Display:

```text
Recommended model
```

with a short explanation.

---

## 8. UI Requirements

Use premium ASTRA UI components.

The screen should feel like a technical lab.

Use:

* AstraCard
* AstraButton
* AstraChip
* AstraMetricCard

Highlight the recommended model visually.

---

## 9. Architecture

Create a domain component:

```kotlin
BenchmarkRunner
```

It must expose:

```kotlin
suspend fun run(request: BenchmarkRequest): BenchmarkReport
```

Create:

* `BenchmarkRequest`
* `BenchmarkReport`
* `BenchmarkResult`
* `BenchmarkRecommendation`

The screen must not compute benchmark results directly.

---

# Out of Scope

Do NOT implement:

* real model execution
* LiteRT benchmark
* ONNX Runtime benchmark
* Core ML benchmark
* persistence
* PDF export
* benchmark history
* cloud comparison
* document RAG

---

# Acceptance Criteria

The task is complete only if:

* Benchmark screen is reachable.
* Prompt can be edited.
* Multiple models can be selected.
* Benchmark can be launched.
* Loading state is displayed.
* Mocked results are displayed.
* Recommendation is displayed.
* Benchmark logic is outside the UI.
* Android builds.
* iOS builds.
* No real AI inference is introduced.

---

# Expected Deliverables

* Functional Benchmark Lab screen.
* Mock benchmark runner.
* Recommendation logic.
* Premium technical UI.

---

# Commit Message

```text
feat: implement benchmark lab
```
