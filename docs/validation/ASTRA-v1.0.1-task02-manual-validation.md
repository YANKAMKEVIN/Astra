# ASTRA v1.0.1 — Task 02 Manual Validation

## Date

2026-06-28

## Device

```text
Model: SM-S921B
Android: 16 / API 36
ABI: arm64-v8a
Connection: USB / ADB
```

## Build installed

```bash
./gradlew :androidApp:installDebug --no-configuration-cache
```

Result:

```text
BUILD SUCCESSFUL
Installed on 1 device.
```

## Launch validation

Command:

```bash
adb shell am start -n com.kevin.astra/.MainActivity
```

Result:

```text
Starting: Intent { cmp=com.kevin.astra/.MainActivity }
```

ADB UI dump confirmed that ASTRA launched successfully and displayed the Dashboard:

```text
Welcome Engineer
Offline Demo Mode
System status
READY
Platform: Android
Device: samsung SM-S921B
```

Recent logcat output did not show an ASTRA `FATAL EXCEPTION` after launch.

## Model bundle availability

Checked path:

```text
shared/src/androidMain/assets/models/litert-lm/
```

Current contents:

```text
README.md
```

No compatible runtime bundle was present:

```text
gemma.task
gemma.litertlm
```

Repository-wide search also found no `.task` or `.litertlm` model bundle.

## Validation status

| Step | Status | Notes |
|---|---:|---|
| Android device detected | Passed | `SM-S921B` connected through ADB. |
| Android app build/install | Passed | Debug APK installed successfully. |
| App launch | Passed | Dashboard visible, no launch crash detected. |
| Real LiteRT-LM bundle present | Blocked | No `gemma.task` or `gemma.litertlm` asset available. |
| Model Manager ready status | Blocked | Requires model bundle in Android assets. |
| Real Assistant generation | Blocked | Requires model bundle in Android assets. |
| Runtime mode `LiteRT-LM Generative Runtime` | Blocked | Requires real generation path. |
| Remove/rename model fallback check | Blocked | Requires first validating with a model present. |

## Remaining manual steps once a model is available

1. Copy the bundle into:

   ```text
   shared/src/androidMain/assets/models/litert-lm/gemma.task
   ```

   or:

   ```text
   shared/src/androidMain/assets/models/litert-lm/gemma.litertlm
   ```

2. Rebuild and install:

   ```bash
   ./gradlew :androidApp:installDebug --no-configuration-cache
   ```

3. In ASTRA Settings, select:

   ```text
   Model: Gemma 3 1B
   Backend: LiteRT-LM
   ```

4. In Model Manager, verify the model is shown as installed/ready.

5. In Assistant, ask:

   ```text
   Explain what Edge AI is in three concise bullet points.
   ```

6. Verify:

   ```text
   Runtime mode: LiteRT-LM Generative Runtime
   Fallback reason: empty
   Response: real generated model text
   Metrics: visible
   ```

7. Remove or rename the model bundle, rebuild/relaunch, and verify fallback:

   ```text
   Fallback Mock Inference
   ```

## Conclusion

Manual Android device installation and launch validation passed.

Full real LiteRT-LM model execution validation is blocked until a compatible Gemma `.task` or `.litertlm` bundle is provided locally. No implementation issue was discovered during the build/install/launch validation.
