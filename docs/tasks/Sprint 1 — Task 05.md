# Sprint 1 — Task 05

# Documents Assistant

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

ASTRA already has:

* Navigation
* Design System
* MVI foundation
* Koin
* Assistant screen
* Settings screen
* Benchmark Lab
* `InferenceEngine`
* `MockInferenceEngine`
* `AskLocalAssistantUseCase`
* in-memory AI configuration
* mocked benchmark architecture

The next objective is to implement a local document assistant.

---

# Goal

Implement the first version of ASTRA’s Documents Assistant.

The goal is to demonstrate a local document question-answering workflow without using cloud services.

This task must simulate a simple local RAG-like workflow using an embedded document.

---

# Scope

Implement only embedded document QA.

No PDF import is required.

---

## 1. Embedded Document

Create one embedded document:

```text
Industrial Pump Maintenance Guide
```

The document must contain realistic sections such as:

* Overview
* Safety Requirements
* Emergency Shutdown Procedure
* Pump Restart Procedure
* Pressure Monitoring
* Common Failure Symptoms
* Maintenance Checklist

The document content can be stored as a Kotlin string or structured static data.

---

## 2. Document State

Create a Documents state containing:

* availableDocuments
* selectedDocument
* documentStatus
* question
* extractedContext
* answer
* isIndexing
* isGenerating
* metrics
* error

---

## 3. Document Status

Support statuses:

* Not Indexed
* Indexed
* Processing

The embedded document can be automatically indexed on screen load or manually via a button.

---

## 4. Local Indexing Simulation

Implement a simple local indexing component.

Create:

```kotlin
DocumentIndexer
```

It must:

* split the embedded document into sections or chunks;
* assign each chunk an id;
* return indexed chunks.

No vector database is required.

No embeddings are required.

---

## 5. Context Retrieval

Create:

```kotlin
DocumentContextRetriever
```

It must:

* receive a user question;
* search indexed chunks using simple keyword matching;
* return the most relevant chunks;
* expose the extracted context to the UI.

---

## 6. Document QA Flow

When the engineer asks a question:

1. Use `DocumentContextRetriever` to extract relevant context.
2. Build a `PromptRequest` using:

   * user question
   * selected industry
   * extracted document context
3. Call the existing `AskLocalAssistantUseCase`.
4. Display answer and metrics.

---

## 7. UI Layout

The screen must contain:

### Header

```text
Documents Assistant
```

Subtitle:

```text
Local document intelligence for critical operations
```

### Document Card

Display:

* document name
* status
* number of chunks
* document size estimate

### Context Card

Display extracted context after a question.

### Question Card

Input placeholder:

```text
Ask a question about the maintenance guide...
```

Button:

```text
Ask Document
```

### Answer Card

Display generated answer.

### Metrics Panel

Display:

* model
* backend
* latency
* tokens/sec

---

# Out of Scope

Do NOT implement:

* PDF import
* file picker
* embeddings
* vector database
* semantic search
* multi-document RAG
* persistence
* cloud services
* real model inference

---

# Acceptance Criteria

The task is complete only if:

* Documents screen is reachable.
* Embedded document is visible.
* Document can be indexed.
* Question can be typed.
* Relevant context is extracted.
* Extracted context is displayed.
* Answer is generated through `AskLocalAssistantUseCase`.
* Metrics are displayed.
* Android builds.
* iOS builds.
* No cloud/network dependency is introduced.

---

# Expected Deliverables

* Functional Documents Assistant screen.
* Embedded maintenance document.
* Simple local indexing.
* Simple context retrieval.
* Integration with existing mock inference architecture.
* Premium ASTRA UI.

---

# Commit Message

```text
feat: implement documents assistant
```
