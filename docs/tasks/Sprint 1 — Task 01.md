# Sprint 1 — Task 01

# Assistant Screen

## Role

You are a Senior Kotlin Multiplatform Engineer working on the ASTRA project.

## References

Before implementing this task, read:

* README.md
* ROADMAP.md
* ENGINEERING_GUIDE.md
* docs/02_Functional_Requirements.md
* docs/03_Platform_Architecture.md
* docs/04_Design_System.md

These documents are the source of truth.

---

# Context

The project foundation has already been established.

The application includes:

* Navigation
* Design System
* Koin
* MVI foundation
* Placeholder screens

Your mission is to implement the **Assistant screen UI**.

No real AI inference must be implemented during this task.

---

# Goal

Build the first functional screen of ASTRA.

The screen must provide the complete user experience for interacting with the future local AI engine while remaining fully connected to the MVI architecture.

The generated response must be mocked for now.

---

# Scope

Implement only the Assistant feature.

---

## Screen Layout

The screen should be divided into the following sections.

### 1. Header

Display:

```
ASTRA Assistant
```

Subtitle:

```
Secure Local AI for Critical Operations
```

---

### 2. Industry Selector

Create a selector allowing the engineer to choose one industry.

Available values:

* Industrial Maintenance
* Aerospace
* Defense
* Energy
* Healthcare

The selected industry must be stored in the screen state.

---

### 3. Prompt Card

Display a premium ASTRA card containing:

Question field

Placeholder:

```
Ask ASTRA about a critical operation...
```

Primary button:

```
Ask ASTRA
```

The button must become disabled while a generation is running.

---

### 4. Loading State

While generating:

* disable controls
* display a progress indicator
* display

```
Generating local response...
```

---

### 5. Response Card

After generation, display a reusable response card.

The card contains:

* title
* generated response
* generation timestamp

The response must support long text.

---

### 6. Metrics Panel

At the bottom of the screen display four metric cards.

Display:

Model

```
Mock Model
```

Backend

```
Mock Engine
```

Latency

```
1.2 s
```

Tokens/sec

```
18
```

Use reusable AstraMetricCard components.

---

# State

The screen state must contain at least:

* selectedIndustry
* question
* response
* isGenerating
* generationTimestamp
* metrics

---

# Intents

Create intents for:

* UpdateQuestion
* SelectIndustry
* AskQuestion
* ClearConversation

---

# Effects

Create effects for:

* ShowError

Only if required.

---

# Mock Behaviour

Do not implement AI.

When the engineer presses **Ask ASTRA**:

Simulate:

* loading delay (~1 second)
* return a realistic industrial answer

Example:

```
Emergency restart procedure

1. Verify that the emergency stop has been released.

2. Check the pressure level.

3. Reset the protection relay.

4. Restart Pump A using Local Mode.

5. Monitor operating pressure for five minutes.

Status:
Pump restarted successfully.
```

Metrics may remain hardcoded.

---

# UI Requirements

Respect the ASTRA Design System.

Use:

* AstraCard
* AstraButton
* AstraMetricCard
* AstraTheme

No Material widgets should be styled manually.

---

# Out of Scope

Do NOT implement:

* LiteRT
* ONNX Runtime
* Core ML
* PromptPipeline
* Repository
* Benchmark
* Documents
* PDF import
* Persistence
* Network

---

# Acceptance Criteria

The task is complete only if:

* Assistant screen is accessible from Dashboard.
* Industry can be selected.
* Question can be typed.
* Ask button starts generation.
* Loading state is visible.
* Mock response is displayed.
* Metrics panel is displayed.
* MVI architecture is respected.
* Android builds.
* iOS builds.
* No architecture violation.

---

# Expected Deliverables

* Fully designed Assistant screen.
* Mock interaction.
* Reusable metrics panel.
* Production-quality UI.

---

# Commit Message

```text
feat: implement assistant screen
```
