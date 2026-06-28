# LiteRT-LM model bundle

Place a compatible Gemma LiteRT-LM / MediaPipe LLM Inference model bundle in this directory when validating ASTRA v1.0.1 real local generation.

Preferred model:

```text
litert-community/Gemma3-1B-IT
```

This Hugging Face repository is gated. You must accept the Gemma license and authenticate before downloading the model.

Preferred filenames supported by ASTRA:

```text
gemma.task
gemma.litertlm
```

Expected local path:

```text
shared/src/androidMain/assets/models/litert-lm/gemma.task
```

or:

```text
shared/src/androidMain/assets/models/litert-lm/gemma.litertlm
```

Legacy split layout is still tolerated by the loader:

```text
model.tflite
tokenizer.model
```

Optional:

```text
config.json
```

Do not commit the model files. They are ignored by `.gitignore` because generative model binaries are large and carry separate license terms.

After adding the bundle, rebuild and install:

```bash
./gradlew :androidApp:installDebug --no-configuration-cache
```

Then select:

```text
Model: Gemma 3 1B
Backend: LiteRT-LM
```

The Assistant metrics should show:

```text
LiteRT-LM Generative Runtime
```
