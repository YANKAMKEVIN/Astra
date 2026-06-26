# Sprint 2 — Task 02

# Prompt Pipeline

## Role

You are a Senior Kotlin Multiplatform Engineer working on the ASTRA project.

## References

Read the project documentation before implementation.

---

# Goal

Introduce ASTRA's Prompt Pipeline.

The objective is to completely isolate prompt construction from the UI and business logic.

Every future AI interaction must go through this pipeline.

---

# Context

The project already contains:

* Assistant
* Documents Assistant
* Benchmark
* MockInferenceEngine
* AskLocalAssistantUseCase
* ModelCatalog

Currently, prompt construction is still performed directly by higher layers.

This responsibility must be moved into a dedicated component.

---

# Scope

Implement only prompt generation.

---

## Create

### PromptBuilder

Responsible for assembling prompts.

It receives:

* engineer question
* selected industry
* selected model
* extracted document context (optional)

It returns:

* final prompt string

---

### PromptTemplate

Create reusable prompt templates for:

* General Assistant
* Industrial Maintenance
* Aerospace
* Defense
* Energy
* Healthcare
* Document QA

---

### PromptPipeline

Responsible for:

* selecting the correct template
* injecting context
* injecting engineer question
* injecting system instructions
* returning the final prompt

---

## Integration

The Assistant ViewModel must no longer build prompts.

The Documents Assistant must no longer build prompts.

The Benchmark feature must use PromptPipeline.

The UseCase must receive an already prepared prompt.

---

## Prompt Rules

Every prompt must contain:

* System role
* Industry persona
* User request
* Context (if available)
* Response formatting instructions

---

## Example

```
You are an industrial maintenance assistant.

Always prioritize safety.

Use the provided maintenance documentation.

If information is missing, clearly say so.

Question:

...

Context:

...
```

---

# Out of Scope

Do NOT implement:

* LiteRT
* ONNX Runtime
* Core ML
* Prompt optimization
* Prompt caching
* Streaming
* AI agents

---

# Acceptance Criteria

* PromptBuilder exists.
* PromptPipeline exists.
* Prompt templates exist.
* Assistant no longer creates prompts.
* Documents Assistant no longer creates prompts.
* Benchmark uses PromptPipeline.
* Android builds.
* iOS builds.

---

# Expected Deliverables

* Centralized prompt architecture.
* Reusable prompt templates.
* Cleaner ViewModels.
* Cleaner UseCases.

---

# Commit Message

```text
feat: introduce prompt pipeline
```
