# ASTRA v1.0.1 — Task 03 Model Bundle Setup

## Goal

Provide a compatible Gemma `.task` or `.litertlm` bundle for real LiteRT-LM validation.

## Preferred source

Official LiteRT community model:

```text
litert-community/Gemma3-1B-IT
```

Expected compatible artifact:

```text
model.litertlm
```

or another Gemma 3 1B IT Android LLM Inference `.task` / `.litertlm` bundle from an approved source.

## Access status

Direct unauthenticated access was checked:

```bash
curl -I -L https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/model.litertlm
```

Result:

```text
HTTP/2 401
x-error-code: GatedRepo
x-error-message: Access to model litert-community/Gemma3-1B-IT is restricted.
You must have access to it and be authenticated to access it.
```

Therefore, Codex cannot download the bundle automatically in this workspace without a user-provided authenticated Hugging Face session/token and accepted Gemma license.

## Local placement

After downloading the model manually, copy it to one of:

```text
shared/src/androidMain/assets/models/litert-lm/gemma.task
shared/src/androidMain/assets/models/litert-lm/gemma.litertlm
```

If the downloaded file is named `model.litertlm`, rename or copy it as:

```text
gemma.litertlm
```

## Git policy

Model bundle extensions are ignored by `.gitignore`:

```text
*.task
*.litertlm
*.tflite
*.bin
*.model
*.spm
```

Do not commit the bundle.

## Validation command

After adding the bundle:

```bash
./gradlew :androidApp:installDebug --no-configuration-cache
```

Then validate on Android:

```text
Settings → Model: Gemma 3 1B
Settings → Backend: LiteRT-LM
Model Manager → status ready
Assistant → Explain what Edge AI is in three concise bullet points.
```

Expected:

```text
Runtime mode: LiteRT-LM Generative Runtime
Fallback reason: empty
Response: real generated text
Metrics: visible
```
