# Sprint 3 — Task 05

# Real Runtime Metrics for Benchmark

## Role

You are a Senior Kotlin Multiplatform Engineer and Edge AI Engineer working on ASTRA.

## References

Before implementing this task, read:

* README.md
* ROADMAP.md
* ENGINEERING_GUIDE.md
* docs/03_Platform_Architecture.md
* docs/06_LiteRT_LM_Evaluation.md
* docs/07_Task_Evaluation_Methodology.md
* existing Benchmark Lab implementation

---

# Context

ASTRA already contains:

* Benchmark Lab
* MockInferenceEngine
* LiteRT runtime foundation
* LiteRT-LM foundation
* RoutingInferenceEngine
* TaskEvaluationEngine
* runtime status display

The Benchmark Lab now has a meaningful task evaluation score.

However, benchmark metrics may still be partially mocked or inconsistent.

---

# Goal

Make Benchmark metrics reflect real runtime execution where available, while keeping clear fallback behavior when real inference is unavailable.

---

# Scope

Implement only Benchmark metric collection and display.

---

## 1. Benchmark Execution Path

Ensure the Benchmark Lab calls the same inference architecture as the Assistant:

```text
Benchmark
  ↓
PromptPipeline
  ↓
RoutingInferenceEngine
  ↓
Selected runtime
  ↓
GenerationResult
  ↓
TaskEvaluationEngine
```

The Benchmark must not generate fake results silently.

---

## 2. Metrics Model

Ensure Benchmark results include:

* model name
* backend name
* runtime mode
* model load time
* inference latency
* total execution time
* time to first token if available
* tokens/sec if available
* memory usage if available
* fallback reason if fallback occurred
* task evaluation score

Unavailable metrics must be displayed as:

```text
N/A
```

Do not invent values.

---

## 3. Runtime Mode

Each benchmark result must clearly display one of:

* Real LiteRT Tensor Runtime
* LiteRT-LM Generative Runtime
* Mock Fallback
* Simulated Local Inference
* Unsupported Platform
* Model Missing

---

## 4. Fallback Transparency

If fallback happens, the UI must show:

* fallback reason
* original selected backend
* actual runtime used

Example:

```text
Selected: LiteRT-LM
Used: Mock Engine
Reason: LiteRT-LM model files missing
```

---

## 5. Recommendation Logic

Update recommendation logic to consider:

1. Task Evaluation Score
2. Real runtime availability
3. Lower total execution time
4. Lower memory usage when available

Prefer real runtime results over mock results when task score is close.

---

## 6. UI Updates

Update Benchmark cards/table to display:

* Task Score
* Runtime Mode
* Latency
* Load Time
* Total Time
* Tokens/sec
* Memory
* Fallback Reason if any

Keep UI clean and readable.

---

## 7. Documentation

Update Benchmark documentation or create:

```text
docs/08_Benchmark_Methodology.md
```

Explain:

* what is measured;
* what is simulated;
* what is unavailable;
* how fallback is represented;
* how recommendations are computed.

---

# Out of Scope

Do NOT implement:

* model download
* multiple real SLM models
* streaming metrics
* battery measurement
* thermal profiling
* cloud comparison
* benchmark history persistence

---

# Acceptance Criteria

The task is complete only if:

* Benchmark uses the real inference architecture.
* Runtime mode is displayed per result.
* Fallback reason is displayed when applicable.
* Fake metrics are removed or clearly marked as simulated.
* Recommendation uses Task Evaluation Score.
* Benchmark methodology documentation exists.
* Android builds.
* iOS builds.
* UI remains decoupled from runtime implementations.

---

# Expected Deliverables

* More honest Benchmark Lab.
* Real/fallback metric display.
* Updated recommendation logic.
* Benchmark methodology documentation.

---

# Commit Message

```text
feat: add real runtime benchmark metrics
```
