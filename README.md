# ASTRA

> **Secure Local AI for Critical Operations**

```{=html}
<p align="center">
```
A cross-platform Edge AI platform built with **Kotlin Multiplatform** to
evaluate, benchmark and demonstrate **Small Language Models (SLMs)**
running **entirely on-device**, without relying on the cloud.
```{=html}
</p>
```

------------------------------------------------------------------------

## ✨ Overview

ASTRA is not a chatbot.

It is an **Edge AI experimentation platform** designed to help
engineers, architects and innovation teams evaluate local AI inference
on Android and iOS.

The project demonstrates how Small Language Models can run directly on a
mobile device while exposing measurable metrics such as latency, memory
usage and inference speed.

------------------------------------------------------------------------

## 🎯 Vision

Modern enterprises increasingly require AI systems that:

-   work offline
-   preserve data privacy
-   reduce cloud costs
-   provide low latency
-   operate inside critical environments

ASTRA demonstrates that these objectives are achievable through **Edge
AI**.

------------------------------------------------------------------------

## 🚀 Key Features

### 🤖 Local AI Assistant

-   Local document assistant
-   Offline-first
-   Industrial personas
-   Streaming responses
-   Prompt pipeline

### 📊 AI Benchmark Lab

Compare multiple models using the same prompt.

Metrics include:

-   Latency
-   Time To First Token
-   Tokens / second
-   Memory usage
-   Backend
-   Device capabilities

### 📄 Document Assistant

-   Local documents
-   Question answering
-   Context injection
-   Offline workflow

### 📱 Device Dashboard

Display:

-   CPU
-   GPU
-   NPU
-   Memory
-   Storage
-   Platform information
-   Current model
-   Current backend

------------------------------------------------------------------------

## Screenshots

> Screenshots will be added after the first stable demo build.

| Dashboard | Assistant | Benchmark |
|:---:|:---:|:---:|
| TBD | TBD | TBD |

------------------------------------------------------------------------

## 🏗 Architecture

ASTRA follows:

-   Kotlin Multiplatform
-   Compose Multiplatform
-   MVI
-   Clean Architecture
-   Koin
-   Offline First
-   SOLID principles

High-level architecture:

``` text
Presentation
        │
     Domain
        │
      Data
        │
 Platform Layer
(Android / iOS)

        │

    AI Core

 ├── InferenceEngine
 ├── ModelCatalog
 ├── BenchmarkRunner
 ├── PromptPipeline
 └── DeviceCapabilityProvider
```

------------------------------------------------------------------------

## 📱 Supported Platforms

  Platform   Status
  ---------- -----------
  Android    ✅
  iOS        🚧
  Desktop    🔮 Future

------------------------------------------------------------------------

## 🤖 Supported Models (Roadmap)

-   Gemma
-   Phi
-   Llama
-   Qwen
-   Mock Model

------------------------------------------------------------------------

## ⚙ Supported Inference Engines

-   LiteRT
-   ONNX Runtime
-   llama.cpp
-   Core ML
-   Mock Engine

------------------------------------------------------------------------

## 🧪 Technology Stack

-   Kotlin Multiplatform
-   Compose Multiplatform
-   Koin
-   Coroutines
-   Kotlin Flow
-   MVI
-   Clean Architecture

------------------------------------------------------------------------

## 📂 Project Structure

``` text
astra/
│
├── README.md
├── ROADMAP.md
├── docs/
│
├── androidApp/
├── iosApp/
│
└── shared/
    ├── core/
    │   ├── ai/
    │   ├── design/
    │   ├── device/
    │   └── navigation/
    │
    ├── data/
    ├── domain/
    ├── presentation/
    └── platform/
```

------------------------------------------------------------------------

## 🛣 Roadmap

### Sprint 0

-   Bootstrap project
-   Design System
-   Navigation
-   Platform architecture

### Sprint 1

-   Dashboard
-   Assistant
-   Settings

### Sprint 2

-   Documents
-   Benchmark

### Sprint 3

-   LiteRT integration
-   Mock inference engine

### Sprint 4

-   ONNX Runtime
-   Core ML
-   Real benchmarks

### Sprint 5

-   Polish
-   Export reports
-   Presentation

------------------------------------------------------------------------

## 🎯 Mission

ASTRA was created to explore the transition from **Cloud AI** to **Edge
AI** by providing a professional experimentation platform capable of
evaluating on-device inference for critical industries such as:

-   Aerospace
-   Defense
-   Energy
-   Healthcare
-   Industrial Maintenance

------------------------------------------------------------------------

## 📖 Documentation

Project documentation is available in the `/docs` directory.

-   Product Vision
-   Functional Requirements
-   Platform Architecture
-   Development Roadmap
-   Design Guidelines

------------------------------------------------------------------------

## 🤝 Philosophy

> **Everything is replaceable.**

Models, inference engines and platform implementations are abstracted
behind clean interfaces to ensure long-term extensibility.

------------------------------------------------------------------------

## 👨‍💻 Author

Developed by Kevin Hermann.

Designed as a showcase project demonstrating modern Mobile Engineering,
Kotlin Multiplatform and Edge AI.
