# ENGINEERING_GUIDE.md

# ASTRA Engineering Guide

> This document is the engineering constitution of ASTRA.

## 1. Mission

Build ASTRA as a production-quality Edge AI platform.

## 2. Source of Truth

Before implementing any task, always read: 1. README.md 2. ROADMAP.md 3.
docs/

These documents are the source of truth.

## 3. Engineering Principles

-   Offline First
-   Clean Architecture
-   MVI
-   SOLID
-   Composition over Inheritance
-   Everything is Replaceable
-   Platform-agnostic Domain

## 4. Architecture Rules

Allowed: - Presentation → Domain - Data → Domain - Platform → Data

Forbidden: - Presentation → Platform - Domain → Android/iOS -
Presentation → Concrete AI Engine

## 5. Code Quality

-   One responsibility per class.
-   Immutable state whenever possible.
-   Prefer interfaces over implementations.
-   Keep files small and readable.

## 6. Naming

Interfaces describe capabilities: - InferenceEngine - ModelCatalog -
PromptPipeline - BenchmarkRunner - DeviceCapabilityProvider

Reusable UI components start with `Astra`.

## 7. Dependency Injection

-   Use Koin.
-   Inject abstractions.
-   Never inject platform implementations into Presentation.

## 8. MVI

Each feature contains: - State - Intent - Effect - ViewModel

Business logic belongs in UseCases.

## 9. AI Rules

-   Models are interchangeable.
-   Engines are interchangeable.
-   MockInferenceEngine is the default development engine.

## 10. Design

-   Always use AstraTheme.
-   Never hardcode colors.
-   Reuse Astra components.

## 11. Git Workflow

-   One Engineering Task = One Commit.
-   Keep the project compiling.
-   Use Conventional Commits.

## 12. Review Checklist

Before merging: - Android builds. - iOS builds (when applicable). -
Architecture respected. - Design System respected. - Acceptance criteria
satisfied.

## 13. AI Instructions

When acting as an AI engineer: - Read the documentation first. - Never
rewrite unrelated code. - Make incremental changes. - Respect the
architecture.

## 14. Definition of Done

A task is done only if: - It compiles. - Acceptance criteria are met. -
No regression is introduced.

## 15. ASTRA Motto

> Everything is replaceable.
