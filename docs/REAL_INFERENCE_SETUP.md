# Real LiteRT Inference Setup

ASTRA supports a local Android LiteRT model at:

```text
shared/src/androidMain/assets/models/astra-slm.tflite
```

## Expected filename

The runtime currently looks for exactly:

```text
astra-slm.tflite
```

inside:

```text
shared/src/androidMain/assets/models/
```

## Compatibility notes

The current Sprint 3 validation path is intentionally minimal:

* Android only;
* one `.tflite` model;
* one input tensor;
* one output tensor;
* numeric tensors supported: `FLOAT32`, `INT32`, `INT64`, `INT16`, `INT8`, `UINT8`, `BOOL`;
* input is zero-filled for runtime validation;
* output is summarized as a tensor preview;
* no tokenizer-heavy SLM text generation yet.

This validates the real LiteRT loading/execution path without introducing a model download manager, tokenizer stack, streaming, or benchmark execution.

## How to verify real inference

1. Place a compatible model here:

   ```text
   shared/src/androidMain/assets/models/astra-slm.tflite
   ```

2. Build Android:

   ```bash
   ./gradlew :androidApp:assembleDebug --no-configuration-cache
   ```

3. In ASTRA Settings, select the LiteRT backend.

4. Ask a question in Assistant.

5. The Assistant metrics card should show:

   ```text
   Real Local Inference
   ```

   and the response should include:

   ```text
   LiteRT local inference completed
   ```

## Fallback behavior

If the model is missing, empty, incompatible, or fails to initialize:

* ASTRA does not crash;
* `MockInferenceEngine` is used automatically;
* the Assistant metrics card shows `Fallback Mock Inference`;
* the fallback reason is displayed in the metrics card.

## Known limitations

* No model download manager.
* No tokenizer-heavy SLM text generation.
* No streaming generation.
* No iOS real inference yet.
* Real LiteRT execution is validated through `Interpreter.run(...)` with numeric tensors.

