# ASTRA Demo Script

This document provides a structured flow for demonstrating ASTRA's capabilities.

## Introduction (30 seconds)
1. **Open ASTRA**: Launch the application.
2. **Splash Screen**: Briefly show the hero splash screen while it "initializes local AI environment".
3. **Objective**: Explain that ASTRA is a secure, offline-first Edge AI platform for critical operations.

## Dashboard (1 minute)
1. **Show Dashboard**: Point out the "Welcome Engineer" message and system status.
2. **Device Capabilities**: Explain how ASTRA detects local CPU, GPU, and NPU availability through the `DeviceCapabilityProvider`.
3. **Explain Offline Mode**: Mention the "Offline Demo Mode" indicator, confirming no data leaves the device.

## Local Assistant (2 minutes)
1. **Navigate to Assistant**: Open the Assistant tab.
2. **Select Scenario**: Choose a curated scenario (e.g., "Restart Pump A" in Industrial Maintenance).
3. **Show Prompt Population**: Explain how selecting a scenario automatically tunes the industry and prompt.
4. **Generate Response**: Tap "Ask ASTRA".
5. **Show Metrics**: Point out the latency, tokens/sec, and memory usage telemetry provided by the `MockInferenceEngine`.

## Documents Assistant (1.5 minutes)
1. **Navigate to Documents**: Open the Documents tab.
2. **Select Document**: Point out the "Industrial Pump Maintenance Guide".
3. **Index Document**: Tap "Index Document" to simulate local RAG processing.
4. **Ask a Question**: Type a question like "What is the emergency shutdown procedure?".
5. **Context Extraction**: Highlight the "Extracted Context" section showing how relevant parts of the document were retrieved locally.

## Benchmark Lab (1.5 minutes)
1. **Navigate to Benchmark**: Open the Benchmark tab.
2. **Select Scenarios/Models**: Pick a few models and a standard prompt.
3. **Run Benchmark**: Tap "Run Benchmark".
4. **Comparison**: Show the comparison table and the "Recommended Model" based on simulated performance.

## Settings & Architecture (1 minute)
1. **Show Settings**: Navigate to Settings to show how AI configuration (temperature, tokens, etc.) is persisted locally.
2. **Explain Architecture**: Summarize the KMP + Clean Architecture foundation that makes all components (models, engines) easily replaceable.

## Conclusion
- Summarize that ASTRA provides a measurable, privacy-preserving alternative to cloud AI.
