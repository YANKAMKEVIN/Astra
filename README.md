# ASTRA

> Secure Local AI for Critical Operations
> Version 1.0.0 release candidate

ASTRA is a cross-platform Edge AI demonstration application built with Kotlin Multiplatform and Compose Multiplatform. It helps engineers and innovation teams evaluate, explain and demonstrate Small Language Model workflows that run locally on-device, with transparent fallback when production model files are not available.

ASTRA is not a general chatbot. It is a technical showcase for offline-first AI architecture, runtime readiness, benchmarking, document grounding and model transparency.

## Project overview

ASTRA demonstrates how critical-operation assistants can be designed without depending on cloud execution by default. The application includes:

- a guided Demo Mode for five-minute stakeholder walkthroughs;
- a device dashboard for platform capability inspection;
- a local assistant backed by real LiteRT-LM inference, with transparent Mock fallback;
- an embedded Documents Assistant for local context retrieval, plus Gmail/email RAG;
- a Vision Assistant that classifies images on-device (Apple Vision / ML Kit);
- a Benchmark Lab with runtime metrics and task evaluation;
- a Model Manager that lists only the models the platform can run and explains readiness/fallback;
- a Project Overview screen for architecture discussions.

The v1.0.0 release candidate is presentation-ready. Real local model downloads (HTTP streaming, progress, cancel/delete, storage accounting) and real generative inference via LiteRT-LM are implemented on **both Android and iOS** — verified end-to-end on Android, while the iOS path builds, downloads and links end-to-end with on-device generation still to be run on a physical device. On-device image classification is real on both platforms. Cloud inference and remote model registries remain out of scope.

## Screenshots

The screenshots below are documentation panels stored in `docs/images/` for release discussions and README previews.

| Splash | Dashboard |
|:--:|:--:|
| ![Splash](docs/images/splash.svg) | ![Dashboard](docs/images/dashboard.svg) |

| Assistant | Documents |
|:--:|:--:|
| ![Assistant](docs/images/assistant.svg) | ![Documents](docs/images/documents.svg) |

| Benchmark | Settings |
|:--:|:--:|
| ![Benchmark](docs/images/benchmark.svg) | ![Settings](docs/images/settings.svg) |

| Demo Mode |
|:--:|
| ![Demo Mode](docs/images/demo-mode.svg) |

## Architecture

ASTRA follows Clean Architecture with an MVI presentation layer:

```text
Presentation
  ├── Compose screens
  ├── immutable State
  ├── typed Intent
  └── ViewModel reducers

Domain
  ├── AI configuration
  ├── benchmark contracts
  ├── document contracts
  ├── task evaluation
  └── model readiness

Data
  ├── static model/backend catalogs
  ├── embedded demo scenarios
  ├── document indexing
  └── persistent settings

Core
  ├── PromptPipeline
  ├── RoutingInferenceEngine
  ├── MockInferenceEngine
  ├── LiteRT foundation
  ├── LiteRT-LM foundation
  └── DeviceCapabilityProvider

Platform
  ├── Android runtime adapters
  └── iOS runtime adapters
```

Key principles:

- Kotlin Multiplatform shared logic and UI;
- Koin dependency injection;
- offline-first behavior;
- replaceable model and backend abstractions;
- transparent fallback instead of fake runtime claims;
- reusable ASTRA design system components.

## Features

### Dashboard

Displays local platform information, memory, storage, supported features and supported inference backends.

### Assistant

Runs curated operational prompts through the prompt pipeline and local inference abstraction. The Mock runtime provides deterministic offline output and metrics for demos.

### Documents

Indexes an embedded maintenance document and retrieves relevant context locally before asking ASTRA.

### Vision Assistant

Classifies a captured image entirely on-device — Apple Vision (`VNClassifyImageRequest`) on iOS, ML Kit image labeling on Android — then feeds the detected labels into the prompt pipeline so the assistant can reason about the image.

### Email & Gmail

Connects Gmail through native OAuth (AppAuth on Android, `ASWebAuthenticationSession` on iOS) and indexes emails and attachments for local RAG alongside PDFs.

### Benchmark

Compares catalog models against demo scenarios and reports latency, time to first token, memory usage, runtime mode and task evaluation quality.

### Task Evaluation

Scores responses against safety, procedure completeness, technical accuracy, domain terminology and clarity.

### Model Manager

Lists only the models the current platform can actually run, shows their readiness, required files, supported backends and expected size, and explains why fallback is active when a local model bundle is missing. Defaults to the real LiteRT-LM runtime when it is detected, otherwise the Mock engine.

### Demo Mode

Guides a stakeholder walkthrough across device capabilities, runtime selection, assistant, documents, benchmark, task evaluation and model manager.

### Project Overview

Provides a read-only technical architecture explorer directly inside the app.

## Supported platforms

| Platform | Status | Notes |
|---|---|---|
| Android | Supported | Compose UI, device capability provider, Mock + real LiteRT-LM runtime, real model download, on-device image classification (ML Kit). |
| iOS | Supported | Compose UI, Mock runtime, real model download, real LiteRT-LM generative inference via a Swift bridge (see below), on-device image classification (Apple Vision). |
| Desktop | Future | Not part of v1.0.0. |

## Runtime and model status

| Runtime | Status |
|---|---|
| Mock Engine | Installed and demo-ready. |
| LiteRT-LM | Real generative local inference on **both** platforms: Android via MediaPipe `LlmInference`, iOS via a community Swift package ([mylovelycodes/LiteRTLM-Swift](https://github.com/mylovelycodes/LiteRTLM-Swift)) bridged into the shared Kotlin code — Google's own official Swift package hit three separate upstream build bugs, documented in [iOS LiteRT-LM Setup](docs/10_iOS_LiteRT_LM_Setup.md). Both load a downloaded or bundled model and generate an actual response to the user's prompt, with real latency/token metrics. The iOS build is confirmed compiling, downloading and linking end-to-end; on-device generation with a real model still needs to be run on a physical device. The default backend when LiteRT-LM is detected. |
| LiteRT (tensor) / ONNX / Core ML / llama.cpp | Removed from the Backend Configuration picker. LiteRT tensor had an Android-only TFLite pass but no generative output; the others never shipped a runtime. Rather than showing them as permanently "unavailable", models that only target them are hidden per platform. The `InferenceBackend` enum keeps them for routing metadata only. |

Model downloads are implemented on both platforms: models are fetched over HTTP with progress reporting, can be cancelled and deleted, non-success responses (404/500) are rejected instead of installing an error body, and on-device storage usage is tracked. Downloaded LiteRT-LM bundles are picked up automatically ahead of any bundled asset. The catalog includes the ungated **Gemma 4 E2B/E4B** LiteRT-LM builds (the model the iOS bridge targets), so a real download works without a HuggingFace token.

## Build instructions

### Requirements

- JDK 17+
- Android Studio or compatible Android SDK
- Xcode for iOS builds on macOS
- Gradle wrapper included in the repository

### Android

```bash
./gradlew :androidApp:assembleDebug --no-configuration-cache
```

### Shared Kotlin Multiplatform checks

```bash
./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test :shared:compileAndroidMain :shared:compileKotlinIosSimulatorArm64 --no-configuration-cache
```

### iOS

```bash
xcodebuild \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -sdk iphonesimulator \
  -configuration Debug \
  -derivedDataPath /tmp/AstraDerivedData \
  CODE_SIGNING_ALLOWED=NO \
  build -quiet
```

## Documentation

- [Product Vision](docs/01_Product_Vision.md)
- [Functional Requirements](docs/02_Functional_Requirements.md)
- [Platform Architecture](docs/03_Platform_Architecture.md)
- [Design System](docs/04_Design_System.md)
- [Edge AI Runtime Evaluation](docs/05_Edge_AI_Runtime_Evaluation.md)
- [LiteRT-LM Evaluation](docs/06_LiteRT_LM_Evaluation.md)
- [Task Evaluation Methodology](docs/07_Task_Evaluation_Methodology.md)
- [Benchmark Methodology](docs/08_Benchmark_Methodology.md)
- [Real Inference Setup](docs/REAL_INFERENCE_SETUP.md)
- [iOS LiteRT-LM Setup](docs/10_iOS_LiteRT_LM_Setup.md)
- [Demo Script](docs/DEMO_SCRIPT.md)

## Future roadmap

ASTRA v1.0.0 is a polished demonstration baseline. Future work may include:

- running the iOS LiteRT-LM Swift bridge on a physical device with a real Gemma 4 model to confirm on-device generation output (see [iOS LiteRT-LM Setup](docs/10_iOS_LiteRT_LM_Setup.md));
- automated test coverage for the platform runtime adapters (download managers, inference engines, readiness providers, image classifiers);
- exportable benchmark reports;
- accessibility pass and localization;
- CI release automation.

## License and usage

No open-source license has been selected yet. Until a license is added, treat ASTRA as an internal demonstration and engineering showcase project.

## Author

Developed by Kevin Hermann as a showcase of modern mobile engineering, Kotlin Multiplatform and Edge AI architecture.
