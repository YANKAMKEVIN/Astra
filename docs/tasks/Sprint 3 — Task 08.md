# Sprint 3 — Task 08

# Project Overview & Architecture Explorer

## Goal

Provide a technical overview of ASTRA directly inside the application.

This screen is intended for demonstrations and technical discussions.

---

## Scope

Create a read-only "Project Overview" screen.

---

### Sections

Display:

#### Architecture

* Clean Architecture
* MVI
* KMP
* Koin
* Prompt Pipeline
* RoutingInferenceEngine

---

#### Runtime

Display:

* Selected Backend
* Current Runtime
* Runtime Status
* Fallback Status

---

#### Models

Display:

* Installed Models
* Model Status
* Quantization
* Context Window

---

#### Device

Display:

* Platform
* CPU
* Memory
* NPU
* Supported Backends

---

#### AI Features

Display implemented capabilities:

* Assistant
* Benchmark
* Documents
* Task Evaluation
* Model Manager

---

#### Documentation

Provide quick access to:

* README
* Architecture
* Benchmark Methodology
* LiteRT Evaluation

---

### Acceptance Criteria

* Overview screen exists.
* Information is read-only.
* Uses existing repositories/services.
* Android builds.
* iOS builds.

---

### Commit Message

```text
feat: add project overview screen
```
