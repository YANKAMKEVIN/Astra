# Sprint 3 — Task 04

# Task Evaluation Engine

## Role

You are a Senior Kotlin Multiplatform Engineer and AI Evaluation Engineer working on ASTRA.

## References

Before implementing this task, read:

* README.md
* ROADMAP.md
* ENGINEERING_GUIDE.md
* docs/02_Functional_Requirements.md
* docs/03_Platform_Architecture.md
* docs/04_Design_System.md
* existing Benchmark Lab implementation

---

# Context

ASTRA already contains:

* Assistant
* Documents Assistant
* Benchmark Lab
* ModelCatalog
* BackendCatalog
* PromptPipeline
* MockInferenceEngine
* LiteRT runtime foundation
* LiteRT-LM foundation
* RoutingInferenceEngine
* runtime status display

The Benchmark Lab currently displays a quality score.

This score must no longer be arbitrary.

It must become a transparent task-based evaluation score.

---

# Goal

Replace the generic `quality score` with a meaningful `Task Evaluation Score`.

The score must explain how well a model response satisfies a business task.

This evaluation must be understandable by engineers, architects and business stakeholders.

---

# Scope

Implement only local deterministic task evaluation.

No LLM-as-a-judge.

No cloud evaluation.

---

## 1. Evaluation Model

Create models:

```kotlin
TaskEvaluationReport
TaskEvaluationCriterion
TaskEvaluationScore
TaskEvaluationBreakdown
```

The report must contain:

* overall score from 0 to 100
* criterion scores
* explanation
* recommendation summary

---

## 2. Evaluation Criteria

For V1, support these criteria:

### Safety

Weight: 30

Evaluates whether the response includes safety-first reasoning, warnings, risk checks or safe operating conditions.

### Procedure Completeness

Weight: 25

Evaluates whether the response covers the expected operational steps.

### Technical Accuracy

Weight: 20

Evaluates whether the response contains technically plausible and non-dangerous instructions.

### Domain Terminology

Weight: 15

Evaluates whether the response uses relevant industry vocabulary.

### Clarity

Weight: 10

Evaluates whether the response is clear, structured and actionable.

Total: 100

---

## 3. Evaluation Engine

Create:

```kotlin
TaskEvaluationEngine
```

It must expose:

```kotlin
fun evaluate(
    prompt: String,
    response: String,
    industry: Industry,
): TaskEvaluationReport
```

For now, the implementation can be deterministic and rule-based.

Example rules:

* Safety score increases if response contains words such as safety, verify, inspect, isolate, warning, emergency, pressure, shutdown.
* Completeness increases if response is structured with steps.
* Technical Accuracy increases if response avoids unsafe wording and includes operational checks.
* Terminology increases if industry-specific keywords are present.
* Clarity increases if response is concise, structured and readable.

---

## 4. Benchmark Integration

Replace `qualityScore` with `TaskEvaluationReport`.

Benchmark results must display:

* overall task score
* Safety score
* Procedure Completeness score
* Technical Accuracy score
* Domain Terminology score
* Clarity score
* short explanation

The recommended model must use the `overall score` instead of arbitrary quality.

---

## 5. Assistant Integration

After generating a response in Assistant, optionally display a compact evaluation summary:

```text
Task Evaluation: 87%
Safety: Strong
Completeness: Good
```

If this creates too much UI complexity, keep the full display only in Benchmark.

---

## 6. UI Requirements

Update labels:

Replace:

```text
Quality
```

with:

```text
Task Evaluation
```

or:

```text
Task Score
```

Add a small explanation:

```text
Measures how well the response satisfies the selected operational task.
```

---

## 7. Documentation

Update or create:

```text
docs/07_Task_Evaluation_Methodology.md
```

Explain:

* what the score represents;
* why it is not an academic benchmark;
* how each criterion is weighted;
* current limitations;
* future improvements.

---

# Out of Scope

Do NOT implement:

* LLM-as-a-judge
* cloud evaluation
* human review workflow
* MMLU/HumanEval scoring
* semantic embeddings
* real benchmark datasets
* persistence of evaluation history

---

# Acceptance Criteria

The task is complete only if:

* `TaskEvaluationEngine` exists.
* Evaluation criteria exist with explicit weights.
* `quality score` is removed or renamed.
* Benchmark uses `TaskEvaluationReport`.
* Recommendation uses task evaluation score.
* UI explains what the score means.
* Documentation exists.
* Android builds.
* iOS builds.
* No cloud/API dependency is introduced.

---

# Expected Deliverables

* Deterministic task evaluation engine.
* Transparent scoring methodology.
* Benchmark UI updated.
* Documentation explaining the score.
* More credible model comparison.

---

# Commit Message

```text
feat: add task evaluation engine
```
