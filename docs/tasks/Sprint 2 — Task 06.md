# Sprint 2 — Task 06

# Demo Scenario Pack

## Role

You are a Senior Kotlin Multiplatform Engineer working on the ASTRA project.

## References

Read the project documentation before implementation.

---

# Goal

Introduce a reusable demo scenario system for ASTRA.

The application must provide ready-to-use industry scenarios so the engineer can demonstrate ASTRA without manually inventing prompts.

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
* Persistent AI configuration

The application is functional, but the demo experience can be improved by adding curated scenarios.

---

# Scope

Implement only local demo scenarios.

No cloud, no remote config.

---

## 1. DemoScenario Model

Create a model:

```kotlin
data class DemoScenario(
    val id: String,
    val title: String,
    val industry: Industry,
    val description: String,
    val prompt: String,
    val expectedValue: String,
)
```

Adapt names to the existing domain model if needed.

---

## 2. DemoScenarioCatalog

Create:

```kotlin
interface DemoScenarioCatalog {
    fun scenarios(): List<DemoScenario>
    fun scenariosForIndustry(industry: Industry): List<DemoScenario>
    fun scenarioById(id: String): DemoScenario?
}
```

Register it with Koin.

---

## 3. Default Scenarios

Create at least 8 scenarios:

### Industrial Maintenance

* Restart Pump A after emergency shutdown
* Diagnose abnormal vibration on conveyor motor

### Aerospace

* Review pre-flight hydraulic system checklist
* Diagnose cockpit sensor inconsistency

### Defense

* Operate in secure offline mode
* Summarize classified-zone maintenance procedure safely

### Energy

* Diagnose pressure anomaly on isolated site
* Prepare turbine inspection checklist

### Healthcare

* Troubleshoot portable medical device alarm
* Verify local-only handling of sensitive patient data

Each scenario must include a realistic prompt and a short explanation of business value.

---

## 4. Assistant Integration

The Assistant screen must allow the engineer to:

* view suggested scenarios;
* select a scenario;
* automatically populate the question field;
* update selected industry according to the scenario.

The engineer must still be able to edit the question manually.

---

## 5. Benchmark Integration

The Benchmark screen must allow the engineer to select a benchmark scenario.

Selecting a scenario must populate the benchmark prompt.

---

## 6. Documents Integration

The Documents screen may display relevant suggested document questions if it fits the current architecture.

This is optional.

---

# Out of Scope

Do NOT implement:

* remote scenario download
* persistence of custom scenarios
* scenario editing
* analytics
* cloud sync
* real AI inference

---

# Acceptance Criteria

The task is complete only if:

* `DemoScenario` exists.
* `DemoScenarioCatalog` exists.
* At least 8 realistic scenarios exist.
* Assistant can select a scenario.
* Selecting a scenario updates industry and prompt.
* Benchmark can select a scenario.
* Android builds.
* iOS builds.
* No network dependency is introduced.

---

# Expected Deliverables

* Local scenario catalog.
* Assistant scenario picker.
* Benchmark scenario picker.
* Improved demo readiness.

---

# Commit Message

```text
feat: add demo scenario catalogg
```
