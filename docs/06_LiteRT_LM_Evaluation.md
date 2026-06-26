# LiteRT-LM Evaluation

## Summary

ASTRA should keep the current LiteRT tensor runtime for low-level `.tflite` validation, and prepare a separate **LiteRT-LM generative runtime** for real SLM text generation.

The existing `Interpreter.run(...)` path is valuable because it proves that Android can package LiteRT native libraries, load local model assets, allocate tensors, execute a local runtime call and return metrics. It is not sufficient for SLM generation because text generation needs a tokenizer, prompt encoding, token sampling, iterative decoding and model-specific configuration.

## Required dependencies

The current ASTRA build already includes:

```kotlin
com.google.ai.edge.litert:litert
```

For full LiteRT-LM generation, ASTRA should add the official Google AI Edge generative runtime dependency once the exact artifact/version is selected for the target model. The integration must remain behind `InferenceEngine`; Presentation must not depend on LiteRT-LM APIs.

## Android support

The runtime should target the same Android baseline as ASTRA:

```text
minSdk 24+
```

Actual accelerator support depends on device/runtime capabilities. CPU fallback should remain available for validation; GPU/NPU delegates can be evaluated after model packaging is stable.

## Supported model formats

Expected LiteRT-LM bundle shape for ASTRA:

```text
shared/src/androidMain/assets/models/litert-lm/
├── model.tflite
├── tokenizer.model
└── config.json       optional
```

The exact filenames may change per model, but the loader now validates the presence of:

* one model file (`.tflite`, `.task`, or `.bin`);
* one tokenizer file (`.model`, `.spm`, or tokenizer-named file);
* optional JSON configuration.

## Recommended model candidates

Candidates should be small enough for a mobile demo:

* Gemma 2B / Gemma 3 1B class models when packaged for LiteRT-LM;
* TinyLlama-class models only if the tokenizer/runtime path is officially supported;
* a purpose-built tiny validation model for CI/demo smoke tests.

The first committed bundle should be license-safe and small enough for Git, or documented as a local-only asset.

## Tokenizer requirements

SLM generation requires:

* prompt normalization;
* tokenizer model loading;
* prompt tokenization;
* generation loop;
* stop-token handling;
* detokenization;
* safety around max token limits.

ASTRA should not implement a tokenizer from scratch in this task. The tokenizer must come from the selected LiteRT-LM runtime or the model bundle.

## Expected model size

Practical demo sizes:

* tiny validation model: KB–tens of MB;
* small quantized SLM: hundreds of MB to several GB;
* production-grade mobile SLM: depends on quantization and context window.

Large binaries should not be committed without an explicit licensing/storage decision.

## Integration complexity

Expected complexity is medium/high:

* asset discovery and validation are simple;
* runtime session lifecycle is manageable;
* tokenizer + sampling loop is model-specific;
* memory and latency vary heavily by device;
* robust errors/fallback are mandatory.

## Limitations

This task does not implement full generation. It adds:

* `LiteRtLmInferenceEngine` skeleton;
* `LiteRtLmModelLoader`;
* Android asset validation;
* runtime statuses for Model Missing / Unsupported Platform / LiteRT-LM Generative Runtime;
* safe fallback to Mock.

