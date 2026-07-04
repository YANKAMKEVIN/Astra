# 10_iOS_LiteRT_LM_Setup.md

# iOS Real Inference Setup (LiteRT-LM Swift API)

## Status

Real generative inference on iOS is implemented via a Kotlin/Swift bridge:

- `shared/src/iosMain/kotlin/com/kevin/astra/core/ai/IosLiteRtLmRuntime.kt` — Kotlin-side
  contract (`NativeLiteRtLmEngine`) plus the `LiteRtLmModelLoader`/`LiteRtLmRuntimeSession`
  implementations that call into it.
- `iosApp/iosApp/LiteRtLmBridge.swift` — Swift implementation of that contract, using the
  community [`LiteRTLMSwift`](https://github.com/mylovelycodes/LiteRTLM-Swift) package (see
  "Why a community fork" below).
- `iosApp/iosApp/ContentView.swift` — registers the bridge before the Compose UI is created.
- `shared/src/iosMain/kotlin/com/kevin/astra/domain/modelmanager/IosModelDownloadManager.kt` —
  real HTTP download via `NSURLSession` (Documents directory, progress, cancel, delete, storage
  accounting), replacing the previous always-fails stub.

This mirrors the Android `LiteRtLmInferenceEngine` path (MediaPipe `LlmInference`).

**Confirmed: `Product > Build` succeeds end-to-end in Xcode** (iOS Simulator destination), verified
live via screen control on the user's Mac — the Kotlin/Native shared module compiles, the
`LiteRTLMSwift` package resolves and links, and `LiteRtLmBridge.swift` compiles against it. The
remaining Xcode issues are pre-existing warnings unrelated to this feature (see "Build history"
below for the one real Kotlin/Native bug this surfaced, and its fix). Not yet verified: runtime
behavior on an actual device with a real `.litertlm` model loaded — that needs a downloaded or
bundled model file and, per the fork's own guidance, a physical device with enough RAM (see below).

## Why a community fork, not Google's official package

Google's own [`google-ai-edge/LiteRT-LM`](https://github.com/google-ai-edge/LiteRT-LM) Swift
package was tried first and hit three real, upstream problems in sequence:

1. Tracking the `main` branch: Bazel's `c/BUILD` file loads a `ios_engine.bzl` that isn't
   published, breaking the iOS build entirely.
2. Pinning the `0.14.0` release tag: SwiftPM refuses "versioned" dependencies whose `Package.swift`
   uses `unsafeFlags` ("The package product 'LiteRTLM' cannot be used as a dependency of this
   target because it uses unsafe build flags"). Workaround: pin the tag's exact commit via a
   **Commit** dependency rule instead of a version rule — SwiftPM only enforces the unsafe-flags
   restriction on version-based dependencies, not branch/commit ones.
3. Even after that, one C symbol (`litert_lm_conversation_render_preface_to_string`, used by a
   debug/logging-only Swift method we never call) was missing from the `0.14.0` release's compiled
   binary — a genuine upstream packaging gap, confirmed to have no known fix at the time of writing.

[`mylovelycodes/LiteRTLM-Swift`](https://github.com/mylovelycodes/LiteRTLM-Swift) ships a
**prebuilt** `CLiteRTLM.xcframework` (built from the same open-source Google C API, Apache 2.0)
rather than building from Bazel source at consumption time, which sidesteps all three problems.
It is not an official Google product — a community wrapper, MIT-licensed, ~10 GitHub stars at
time of writing. Its Swift API is also **different** from Google's official one: no
`Engine`/`Conversation`/`Message` types, instead `LiteRTLMEngine.generate(prompt:temperature:maxTokens:)`
returning a plain `String`, and text generation requires Gemma 4's native turn-marker format
(`<|turn>user\n...\n<turn|>\n<|turn>model\n`) — `LiteRtLmBridge.swift` wraps this internally so
the shared Kotlin code doesn't need to know about it.

If Google fixes the official package, switching back means only rewriting `LiteRtLmBridge.swift`
and the package reference — the Kotlin side (`IosLiteRtLmRuntime.kt`) doesn't need to change.

## One-time manual steps: add the LiteRT-LM Swift package + entitlement

This can't be done by hand-editing `project.pbxproj` reliably — do it once in Xcode:

1. Open `iosApp/iosApp.xcodeproj` in Xcode.
2. **File > Add Package Dependencies...**
3. Enter `https://github.com/mylovelycodes/LiteRTLM-Swift.git`.
4. Select the **LiteRTLMSwift** library (not `LiteRTLM`) and add it to the `iosApp` target.
5. If Xcode reports "no such module LiteRTLMSwift": select the `iosApp` target → **General** tab →
   **Frameworks, Libraries, and Embedded Content** → **+** → the package's product.
6. Add the **Increased Memory Limit** entitlement: `iosApp` target → **Signing & Capabilities** →
   **+ Capability** → search "Increased Memory Limit". Model loading needs ~4 GB RAM; without this
   entitlement iOS may kill the app during load. (Equivalent manual entitlement key:
   `com.apple.developer.kernel.increased-memory-limit` = `true`.)

The project's iOS deployment target is already 18.2, above the fork's iOS 17.0+ requirement. Note
the fork's own guidance: Gemma 4 E2B (~2.6 GB model) wants an iPhone 13 Pro or later with 6 GB+ RAM
for on-device testing — the Simulator may work for basic smoke-testing but isn't representative of
real performance or memory behavior.

## Adding a demo model

LiteRT-LM expects a `.litertlm` file. Two ways to get one on-device:

1. **Download at runtime** (recommended): use the existing Model Manager screen — the download
   manager now performs a real fetch into the app's Documents directory. Point `LocalModel.downloadUrl`
   at a `.litertlm` asset (e.g. from a HuggingFace `litert-community` repo).
2. **Bundle for a quick demo**: drag a `.litertlm` file (e.g. named `gemma.litertlm`) into the
   `iosApp` target in Xcode ("Copy items if needed", target membership checked). `IosLiteRtLmModelLoader`
   looks it up via `NSBundle.mainBundle.pathForResource(_:ofType:"litertlm")`, trying the model's
   `filesystemId` first, then falling back to a file literally named `gemma.litertlm`.

## Build history (live debugging session)

Verified live, in Xcode, on the user's Mac via screen control:

- The package resolves and the "unsafe build flags" restriction is confirmed gone once switched
  from `google-ai-edge/LiteRT-LM` to the community fork (prebuilt binary, no `unsafeFlags` in its
  `Package.swift`).
- `Product > Build` now succeeds end-to-end (iOS Simulator destination): the Kotlin/Native shared
  module compiles, `LiteRtLmBridge.swift` compiles against `LiteRTLMSwift`, and the app links.
- One real Kotlin/Native compiler bug surfaced and got fixed along the way, in
  `IosModelDownloadManager.kt`: setting the download's `Authorization` header directly on an
  `NSMutableURLRequest` failed to resolve under **three** different approaches —
  `setValue(value:forHTTPHeaderField:)` on a request built via the `initWithURL:`-style
  constructor, assigning the `allHTTPHeaderFields` property, and `setValue(...)` again on a request
  built via the `requestWithURL(_:)` factory method plus an explicit `as NSMutableURLRequest` cast.
  All three gave "Unresolved reference" from the Kotlin/Native compiler — the header-field API
  surface simply isn't resolving on this class in this project's cinterop binding, for reasons that
  didn't matter enough to chase further once a clean workaround was in hand: the fix sets the
  `Authorization` header on the **session's** `NSURLSessionConfiguration.HTTPAdditionalHeaders`
  dictionary instead of the request object, which every request made through that session picks up
  automatically. `NSMutableURLRequest.requestWithURL(_:)` itself does correctly type its return as
  `NSMutableURLRequest` (confirmed by Xcode flagging the explicit cast as an unnecessary "No cast
  needed" warning), so that part of the original "instancetype covariance" concern was unfounded.

## Known risk areas (verify further on-device)

- **`LiteRTLMEngine.generate` signature**: compiles cleanly against the fork's actual interface, so
  the parameter names/labels in `LiteRtLmBridge.swift` are confirmed correct. Not yet verified:
  whether it produces sensible output at runtime — that needs a real `.litertlm` model and a device
  run.
- **Gemma 4 model requirement**: the fork's `generate()` expects a Gemma 4 model specifically (turn
  markers are Gemma-4-native); other models in `AiModel` (Phi, Llama, Qwen, SmolLM) are unlikely to
  produce sensible output through this path even if a `.litertlm` file is present.
- **Device RAM**: Gemma 4 E2B (~2.6 GB model) wants an iPhone 13 Pro or later with 6 GB+ RAM for
  on-device testing — the Simulator may build and launch but isn't representative of real
  performance or memory behavior under load.
- **iOS-only path**: only the LiteRT-LM (generative) engine got a real iOS implementation. Plain
  LiteRT (the non-generative tensor path used for the Android tensor-shape demo) remains
  Android-only; iOS keeps using Mock fallback there — that was out of scope for this pass.
