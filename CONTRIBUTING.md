# Contributing to ASTRA

ASTRA is currently an internal demonstration and engineering showcase project. Contributions should preserve the release-quality demo experience and the architecture boundaries already in place.

## Architecture

ASTRA follows Clean Architecture:

- `presentation`: Compose screens, state, intents and ViewModels.
- `domain`: interfaces, use cases and business models.
- `data`: repositories, static catalogs, local indexing and persistence.
- `core`: design system, navigation, device capability and AI runtime abstractions.
- platform source sets: Android/iOS adapters and actual implementations.

Presentation code must not depend directly on Android or iOS platform implementations. Inject abstractions through Koin.

## Coding conventions

- Prefer immutable state and typed intents.
- Keep screens read-only unless the task explicitly requires interaction.
- Use `AstraCard`, `AstraButton`, `AstraChip`, `AstraMetricCard`, `AstraScreen` and `AstraTheme`.
- Do not hardcode colors outside the design system.
- Keep fallback behavior explicit; never claim a production model is installed unless local files are present.
- Add or update tests when behavior changes.
- Keep one engineering task focused and reviewable.

## Branch naming

Use descriptive branches:

```text
feat/<short-feature-name>
fix/<short-bug-name>
chore/<maintenance-name>
docs/<documentation-name>
```

Codex-generated branches should use the `codex/` prefix when branch creation is needed.

## Commit messages

Use Conventional Commits:

```text
feat: add model manager
fix: correct benchmark readiness state
docs: update release documentation
chore: prepare ASTRA v1.0.0 release
```

## Pull request expectations

Each pull request should include:

- summary of changes;
- screenshots or documentation images when UI changes;
- tests/build commands run;
- known limitations;
- confirmation that no unrelated files were modified.

Before merging, run:

```bash
./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test :shared:compileAndroidMain :shared:compileKotlinIosSimulatorArm64 --no-configuration-cache
./gradlew :androidApp:assembleDebug --no-configuration-cache
```

For iOS release confidence, also run:

```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -configuration Debug -derivedDataPath /tmp/AstraDerivedData CODE_SIGNING_ALLOWED=NO build -quiet
```
