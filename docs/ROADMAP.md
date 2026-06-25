# ROADMAP.md

# ASTRA Development Roadmap

> **Project:** ASTRA\
> **Slogan:** Secure Local AI for Critical Operations

This roadmap is the single source of truth for the development of ASTRA.

------------------------------------------------------------------------

# Product Goal

Build a premium cross-platform Edge AI platform demonstrating local
inference of Small Language Models on Android and iOS.

------------------------------------------------------------------------

# MVP Success Criteria

-   Android & iOS application (KMP)
-   Premium UI
-   Offline-first
-   Mock AI engine
-   Plug-in architecture for inference engines
-   AI Benchmark
-   Document Assistant
-   Dashboard
-   Settings
-   Clean Architecture + MVI + Koin

------------------------------------------------------------------------

# Sprint 0 --- Platform Bootstrap

## Goal

Create a stable foundation.

### Tasks

-   [ ] Create KMP project
-   [ ] Configure Android target
-   [ ] Configure iOS target
-   [ ] Configure Compose Multiplatform
-   [ ] Configure Gradle Convention
-   [ ] Create Design System
-   [ ] Dark Theme
-   [ ] Typography
-   [ ] Colors
-   [ ] Navigation
-   [ ] Splash Screen
-   [ ] Dashboard placeholder
-   [ ] Project README
-   [ ] Documentation structure

### Definition of Done

-   Project compiles
-   Android launches
-   iOS launches
-   Navigation works

------------------------------------------------------------------------

# Sprint 1 --- Core Architecture

## Goal

Implement production-ready architecture.

### Tasks

-   [ ] MVI infrastructure
-   [ ] Clean Architecture
-   [ ] Koin
-   [ ] State management
-   [ ] Navigation abstraction
-   [ ] Error handling
-   [ ] Logging
-   [ ] App settings

### Deliverables

-   Stable architecture
-   Reusable base classes

------------------------------------------------------------------------

# Sprint 2 --- Dashboard

## Features

-   [ ] Welcome Engineer
-   [ ] Device information
-   [ ] CPU
-   [ ] GPU
-   [ ] NPU detection (best effort)
-   [ ] Memory
-   [ ] Storage
-   [ ] Current model
-   [ ] Current backend
-   [ ] Capabilities cards
-   [ ] Quick actions

------------------------------------------------------------------------

# Sprint 3 --- Assistant

## Features

-   [ ] Ask ASTRA
-   [ ] Industry selector
-   [ ] Prompt pipeline
-   [ ] Mock inference
-   [ ] Streaming UI
-   [ ] Metrics panel
-   [ ] Conversation history (current session)

------------------------------------------------------------------------

# Sprint 4 --- Documents

## Features

-   [ ] Embedded sample document
-   [ ] Local indexing
-   [ ] Context extraction
-   [ ] Document QA
-   [ ] Offline workflow

Stretch:

-   [ ] PDF import

------------------------------------------------------------------------

# Sprint 5 --- Benchmark Lab

## Features

-   [ ] Model selector
-   [ ] Backend selector
-   [ ] Prompt selection
-   [ ] Run benchmark
-   [ ] Comparison table
-   [ ] Recommendation engine

Metrics:

-   Latency
-   Time To First Token
-   Tokens/sec
-   Memory
-   Backend
-   Model

------------------------------------------------------------------------

# Sprint 6 --- AI Engines

## Mock Engine

-   [ ] Generate realistic responses
-   [ ] Fake metrics

## LiteRT

-   [ ] Integration
-   [ ] Model loading
-   [ ] Local inference

## ONNX Runtime

-   [ ] Integration
-   [ ] Benchmark support

## Core ML (iOS)

-   [ ] Architecture ready
-   [ ] Initial integration

------------------------------------------------------------------------

# Sprint 7 --- Polish

-   [ ] Animations
-   [ ] Hero splash
-   [ ] Empty states
-   [ ] Loading states
-   [ ] Error screens
-   [ ] Accessibility
-   [ ] Performance review

------------------------------------------------------------------------

# Documentation

-   [ ] Product Vision
-   [ ] Functional Requirements
-   [ ] Platform Architecture
-   [ ] Design Guidelines
-   [ ] AI Architecture
-   [ ] Benchmark Methodology
-   [ ] Demo Guide

------------------------------------------------------------------------

# Report Deliverables

-   [ ] Technology watch report
-   [ ] SLM comparison
-   [ ] NPU comparison
-   [ ] Cloud vs Edge matrix
-   [ ] POC recommendations
-   [ ] Presentation slides

------------------------------------------------------------------------

# Nice to Have (V2)

-   [ ] Vision models
-   [ ] OCR
-   [ ] Speech-to-Text
-   [ ] Text-to-Speech
-   [ ] Multi-document RAG
-   [ ] PDF export
-   [ ] Benchmark history
-   [ ] Desktop target

------------------------------------------------------------------------

# Engineering Principles

-   Everything is replaceable.
-   Offline first.
-   Platform before features.
-   Benchmark everything.
-   Clean Architecture.
-   SOLID.
-   Testability.
-   Production-quality code.

------------------------------------------------------------------------

# Commit Strategy

-   One feature per commit.
-   Always keep the project compiling.
-   Document architectural decisions.
-   Review every generated change before merging.

------------------------------------------------------------------------

# Daily Plan (7-Day Challenge)

## Day 1

-   Bootstrap
-   README
-   Roadmap
-   Architecture

## Day 2

-   Design System
-   Navigation
-   Dashboard

## Day 3

-   Assistant
-   Mock Engine

## Day 4

-   Documents
-   Benchmark

## Day 5

-   LiteRT / ONNX integration

## Day 6

-   Polish
-   Report
-   Slides

## Day 7

-   Demo rehearsal
-   Final fixes
-   GitHub cleanup

------------------------------------------------------------------------

# Final Demo Checklist

-   [ ] Splash
-   [ ] Dashboard
-   [ ] Assistant
-   [ ] Documents
-   [ ] Benchmark
-   [ ] Settings
-   [ ] Mock engine
-   [ ] Real engine (if available)
-   [ ] Android demo
-   [ ] iOS demo
-   [ ] Report
-   [ ] Presentation

------------------------------------------------------------------------

> **Mission Statement**

ASTRA demonstrates how Edge AI can become a practical, secure and
measurable alternative to cloud inference for critical operations.
