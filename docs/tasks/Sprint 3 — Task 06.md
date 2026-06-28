# Sprint 3 — Task 06

# Model Manager

## Role

You are a Senior Kotlin Multiplatform Engineer and Edge AI Engineer working on ASTRA.

## References

Before implementing this task, read:

* README.md
* ROADMAP.md
* ENGINEERING_GUIDE.md
* docs/03_Platform_Architecture.md
* docs/06_LiteRT_LM_Evaluation.md
* docs/08_Benchmark_Methodology.md
* existing ModelCatalog and BackendCatalog implementations

---

# Context

ASTRA now contains:

* ModelCatalog
* BackendCatalog
* LiteRT runtime foundation
* LiteRT-LM foundation
* RoutingInferenceEngine
* Benchmark runtime metrics
* Task Evaluation Engine

Some real runtimes require local model files.

Currently, model availability may be understood internally but is not clearly exposed to the engineer.

---

# Goal

Create a Model Manager experience that shows model availability, runtime readiness and required files.

ASTRA must clearly explain whether a model is:

* installed
* available
* missing required files
* unsupported
* future/coming soon

---

# Scope

Implement only local model status visualization and readiness checks.

---

## 1. Model Readiness Model

Create or extend model metadata with:

* model id
* display name
* provider
* parameter count
* quantization
* expected size
* supported backends
* required files
* local path
* status
* readiness message

---

## 2. Model Status

Support clear statuses:

* Installed
* Model Required
* Missing Files
* Unsupported Platform
* Coming Soon

---

## 3. LiteRT-LM Readiness

For LiteRT-LM models, check expected folder:

```text
shared/src/androidMain/assets/models/litert-lm/
```

Detect expected files when possible:

* model file
* tokenizer file
* config file

If files are missing, show exactly what is missing.

---

## 4. UI Integration

Add a Model Manager section either:

* inside Settings; or
* as a dedicated screen if navigation already supports it cleanly.

The UI must display:

* model name
* provider
* status
* expected size
* supported backend
* required files
* current readiness message

---

## 5. Actions

For V1, actions can be non-functional but explicit:

* “View setup guide”
* “Model download coming soon”
* “Use Mock fallback”

Do not implement model download yet.

---

## 6. Documentation Link

If setup documentation exists, reference:

```text
docs/REAL_INFERENCE_SETUP.md
```

inside the UI text or developer note.

---

# Out of Scope

Do NOT implement:

* model download manager
* remote model registry
* Hugging Face integration
* file picker
* model deletion
* real Core ML integration
* real ONNX integration

---

# Acceptance Criteria

The task is complete only if:

* Model readiness is visible to the engineer.
* LiteRT-LM missing files are clearly displayed.
* Settings or Model Manager shows model statuses.
* The user can understand why fallback is active.
* No fake model installation is shown.
* Android builds.
* iOS builds.
* Architecture remains clean.

---

# Expected Deliverables

* Model Manager UI.
* Model readiness checks.
* Clear local model status.
* Improved demo transparency.

---

# Commit Message

```text
feat: add model manager
```
