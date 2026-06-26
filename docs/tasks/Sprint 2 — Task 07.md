# Sprint 2 — Task 07

# Demo Polish & Presentation Readiness

## Role

You are a Senior Kotlin Multiplatform Engineer working on the ASTRA project.

## References

Read the project documentation before implementation.

---

# Goal

Prepare ASTRA for a professional demo.

The application must feel cohesive, polished and presentation-ready across all existing screens.

---

# Context

ASTRA already contains:

* Foundation
* Assistant
* Mock Inference Engine
* Settings
* Benchmark Lab
* Documents Assistant
* Model Catalog
* Backend Catalog
* Prompt Pipeline
* Device Capabilities
* Persistent AI Configuration
* Demo Scenario Catalog

The application is now functionally rich enough to be demonstrated.

This task focuses on polish, consistency and demo readiness.

---

# Scope

Implement only UI polish and presentation improvements.

---

## 1. App-wide Visual Consistency

Review all screens and ensure they consistently use:

* AstraTheme
* AstraCard
* AstraButton
* AstraChip
* AstraMetricCard
* consistent spacing
* consistent typography
* consistent section titles

No screen should feel visually disconnected from the others.

---

## 2. Demo Mode Indicator

Add a subtle app-wide indicator showing:

```text
Offline Demo Mode
```

It should appear on key screens:

* Dashboard
* Assistant
* Documents
* Benchmark
* Settings

The indicator must not be intrusive.

---

## 3. Empty & Loading States

Improve empty/loading states for:

* Assistant
* Documents
* Benchmark
* Settings if needed

Each state must have:

* clear title
* short explanation
* visually polished layout

---

## 4. Error States

Create or improve a reusable ASTRA error component.

Use it when:

* question is empty
* no model selected
* no backend selected
* benchmark has no selected model
* document is not indexed

---

## 5. Screenshot Readiness

Ensure each main screen looks good when captured for GitHub README and presentation slides.

Screens to optimize:

* Splash
* Dashboard
* Assistant
* Documents
* Benchmark
* Settings

---

## 6. README Screenshot Placeholders

Update README.md with a section:

```markdown
## Screenshots

> Screenshots will be added after the first stable demo build.

| Dashboard | Assistant | Benchmark |
|---|---|---|
| TBD | TBD | TBD |
```

Do not add fake image paths unless assets already exist.

---

## 7. Demo Script Placeholder

Create:

```text
docs/DEMO_SCRIPT.md
```

The file must contain a first draft of the demo flow:

1. Open ASTRA
2. Show Dashboard
3. Explain offline mode
4. Select a scenario in Assistant
5. Generate local response
6. Open Documents Assistant
7. Ask a document question
8. Run Benchmark
9. Show Settings
10. Explain architecture

Keep it concise.

---

# Out of Scope

Do NOT implement:

* real inference
* LiteRT
* ONNX
* Core ML
* PDF export
* actual screenshots
* video recording
* analytics

---

# Acceptance Criteria

The task is complete only if:

* All screens feel visually consistent.
* Offline Demo Mode indicator exists.
* Empty/loading states are polished.
* Reusable error component exists or is improved.
* README contains a screenshot placeholder section.
* `docs/DEMO_SCRIPT.md` exists.
* Android builds.
* iOS builds.
* No real inference is introduced.

---

# Expected Deliverables

* Polished demo-ready UI.
* Better presentation flow.
* README screenshot section.
* Demo script draft.

---

# Commit Message

```text
chore: polish demo experience
```
