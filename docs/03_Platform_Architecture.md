# 03_Platform_Architecture.md

# ASTRA Platform Architecture (RFC-001)

> **Version:** 1.0 **Status:** Approved **Project:** ASTRA --- Secure
> Local AI for Critical Operations

------------------------------------------------------------------------

# 1. Purpose

This document defines the reference architecture of ASTRA.

Its objective is to provide a scalable, maintainable and testable
architecture for a cross-platform Edge AI platform built with Kotlin
Multiplatform.

The architecture must support multiple AI models, multiple inference
engines and multiple platforms without impacting the business layer.

------------------------------------------------------------------------

# 2. Architectural Principles

-   Offline First
-   Clean Architecture
-   MVI
-   SOLID
-   Platform Agnostic Domain
-   Everything is Replaceable
-   Testability First
-   Plugin-Oriented AI Layer

------------------------------------------------------------------------

# 3. Technology Stack

  Layer            Technology
  ---------------- ----------------------------------------
  Language         Kotlin
  Cross Platform   Kotlin Multiplatform
  UI               Compose Multiplatform
  Architecture     Clean Architecture + MVI
  DI               Koin
  Async            Coroutines + Flow
  Persistence      Multiplatform Settings (or equivalent)
  AI Engines       LiteRT / ONNX Runtime / Core ML / Mock
  Models           Gemma / Phi / Llama / Qwen / Mock

------------------------------------------------------------------------

# 4. High Level Architecture

``` text
Presentation
      │
Domain
      │
Data
      │
Platform
      │
Core AI
```

The UI never depends on a concrete inference engine.

------------------------------------------------------------------------

# 5. Project Structure

``` text
astra/
│
├── androidApp/
├── iosApp/
│
└── shared/
    ├── core/
    │   ├── ai/
    │   ├── design/
    │   ├── device/
    │   ├── navigation/
    │   └── utils/
    │
    ├── presentation/
    │   ├── dashboard/
    │   ├── assistant/
    │   ├── benchmark/
    │   ├── documents/
    │   └── settings/
    │
    ├── domain/
    ├── data/
    └── platform/
```

------------------------------------------------------------------------

# 6. Core AI

The AI layer is based on abstractions.

## InferenceEngine

Responsible for inference only.

Possible implementations:

-   MockInferenceEngine
-   LiteRtInferenceEngine
-   OnnxInferenceEngine
-   CoreMLInferenceEngine

## ModelCatalog

Responsibilities:

-   available models
-   current model
-   install/remove models
-   model metadata

## PromptPipeline

Responsibilities:

-   build prompts
-   inject persona
-   inject document context
-   prepare final request

## BenchmarkRunner

Responsibilities:

-   execute benchmarks
-   collect metrics
-   compute recommendation

## DeviceCapabilityProvider

Responsibilities:

-   CPU
-   GPU
-   NPU
-   Memory
-   Storage
-   Platform features

------------------------------------------------------------------------

# 7. Clean Architecture

Presentation

-   Screens
-   State
-   Intent
-   Effect
-   ViewModel

Domain

-   UseCases
-   Repository contracts
-   Business models

Data

-   Repository implementations
-   Prompt builders
-   Persistence
-   AI orchestration

Platform

-   Android implementation
-   iOS implementation

------------------------------------------------------------------------

# 8. MVI

Every screen owns:

State

Intent

Effect

No business logic inside Composables.

------------------------------------------------------------------------

# 9. Dependency Rule

Allowed:

Presentation → Domain

Domain → nothing

Data → Domain

Platform → Data

Forbidden:

Presentation → AI Engine

Presentation → Platform

Domain → Android

Domain → iOS

------------------------------------------------------------------------

# 10. AI Flow

``` text
Engineer
    │
Assistant Screen
    │
ViewModel
    │
AskAssistantUseCase
    │
Repository
    │
PromptPipeline
    │
InferenceEngine
    │
Response
```

------------------------------------------------------------------------

# 11. Platform Strategy

Android:

-   LiteRT
-   ONNX Runtime
-   Mock

iOS:

-   Core ML
-   Mock

Both share the same domain layer.

------------------------------------------------------------------------

# 12. Design Goals

The application must look like an industrial control platform.

Characteristics:

-   Dark theme
-   Premium cards
-   Smooth animations
-   Glassmorphism (light)
-   Dashboard-oriented UI

------------------------------------------------------------------------

# 13. Testing Strategy

Unit Tests

-   UseCases
-   PromptPipeline
-   BenchmarkRunner

UI Tests

-   Dashboard
-   Assistant
-   Benchmark

Integration

-   MockInferenceEngine

------------------------------------------------------------------------

# 14. Extensibility

Adding a new model must not require modifying Presentation.

Adding a new inference backend must only require implementing
InferenceEngine.

Adding a new platform must not affect Domain.

------------------------------------------------------------------------

# 15. Architecture Decision Record

ADR-001

The application is platform-first, not model-first.

ADR-002

The domain is independent of every AI framework.

ADR-003

MockInferenceEngine is the default engine during development.

ADR-004

Every feature must be demonstrable independently.

------------------------------------------------------------------------

# 16. Future Evolution

Planned extensions:

-   Vision
-   OCR
-   Speech-to-Text
-   Text-to-Speech
-   Local RAG
-   Desktop target
-   Export Benchmark PDF

------------------------------------------------------------------------

# 17. Success Criteria

A successful architecture allows:

-   replacing LiteRT with ONNX without UI changes;
-   replacing Gemma with Phi without business changes;
-   sharing more than 90% of the code between Android and iOS;
-   keeping every screen testable;
-   supporting future AI capabilities with minimal refactoring.
