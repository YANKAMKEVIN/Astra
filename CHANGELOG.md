# Changelog

All notable changes for ASTRA are summarized here.

## 1.0.0 Release Candidate

ASTRA v1.0.0 prepares the project for a public-quality demonstration.

### Sprint 1 — Core application shell

- Established the Kotlin Multiplatform application structure.
- Added the ASTRA design system, dark visual language and reusable UI components.
- Implemented navigation, splash, dashboard, assistant and settings foundations.
- Introduced MVI state management and Koin dependency injection.
- Added persistent AI configuration for model, backend and prompt parameters.

### Sprint 2 — AI workflows

- Added model and backend catalogs.
- Introduced the prompt pipeline and industry-aware prompt configuration.
- Implemented the Documents Assistant with embedded maintenance content and local retrieval.
- Added the Benchmark Lab with runtime metrics and model comparison.
- Added task evaluation scoring for safety, completeness, technical accuracy, terminology and clarity.
- Expanded tests around assistant, documents, benchmark and settings behavior.

### Sprint 3 — Edge AI transparency and release readiness

- Added Mock, LiteRT and LiteRT-LM runtime foundations with routing and fallback transparency.
- Added real-inference setup documentation and LiteRT-LM readiness checks.
- Added Model Manager to show required files, statuses and local paths.
- Added Demo Mode for guided stakeholder walkthroughs.
- Added Project Overview for architecture discussions inside the app.
- Prepared release documentation, screenshots, contribution guide and version 1.0.0 metadata.

### Validation

- Android debug build passes.
- iOS simulator build passes.
- Shared Android host tests pass.
- Shared iOS simulator tests pass.
