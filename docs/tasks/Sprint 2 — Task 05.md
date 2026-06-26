# Sprint 2 — Task 05

# Persistent AI Configuration

## Role

You are a Senior Kotlin Multiplatform Engineer working on the ASTRA project.

## References

Read the project documentation before implementation.

---

# Goal

Persist the AI configuration selected by the engineer.

ASTRA must remember the selected model, backend and inference parameters after app restart.

---

# Context

ASTRA already contains:

* Assistant
* Documents Assistant
* Benchmark Lab
* Settings
* ModelCatalog
* BackendCatalog
* PromptPipeline
* DeviceCapabilityProvider
* in-memory AI configuration

The current AI configuration may be stored only in memory.

This task must introduce persistence in a clean multiplatform way.

---

# Scope

Implement only local persistence for AI configuration.

---

## 1. AI Configuration Model

Ensure the configuration contains:

* selectedModelId
* selectedBackendId
* selectedIndustry
* temperature
* maxTokens
* contextWindow
* quantization
* experimentalFeaturesEnabled

---

## 2. Configuration Repository

Create or refactor:

```kotlin
AiConfigurationRepository
```

It must expose:

```kotlin
fun observeConfiguration(): Flow<AiConfiguration>

suspend fun getConfiguration(): AiConfiguration

suspend fun updateConfiguration(configuration: AiConfiguration)

suspend fun updateSelectedModel(modelId: String)

suspend fun updateSelectedBackend(backendId: String)
```

---

## 3. Local Data Source

Create a local data source for persistence.

Preferred options:

* Multiplatform Settings if already available
* DataStore on Android + NSUserDefaults on iOS if already structured
* A simple expect/actual key-value storage abstraction

Do not introduce a new library unless necessary.

---

## 4. Default Values

If no saved configuration exists, use:

* selectedModelId: Mock Model id
* selectedBackendId: Mock Engine id
* selectedIndustry: Industrial Maintenance
* temperature: 0.3
* maxTokens: 512
* contextWindow: 4096
* quantization: 4-bit
* experimentalFeaturesEnabled: false

---

## 5. Settings Integration

Settings must:

* read current configuration from `AiConfigurationRepository`;
* update repository when values change;
* reflect saved values after app restart.

---

## 6. Assistant Integration

Assistant must use persisted configuration when building the prompt request.

---

## 7. Benchmark Integration

Benchmark must use persisted configuration as default backend/model context where applicable.

---

# Out of Scope

Do NOT implement:

* cloud sync
* account system
* remote config
* encrypted storage
* model download
* real inference backend switching

---

# Acceptance Criteria

The task is complete only if:

* `AiConfigurationRepository` exposes a Flow.
* Configuration is persisted locally.
* Settings updates are persisted.
* Restarting the app keeps selected configuration.
* Assistant uses persisted configuration.
* Benchmark uses persisted configuration where relevant.
* Android builds.
* iOS builds.
* No cloud/network dependency is introduced.

---

# Expected Deliverables

* Persistent AI configuration.
* Clean repository abstraction.
* Local storage implementation.
* Settings refactored to use persisted state.

---

# Commit Message

```text
feat: persist AI configuration
```
