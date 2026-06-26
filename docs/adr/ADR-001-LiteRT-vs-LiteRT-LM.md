# ADR-001: LiteRT tensor runtime vs LiteRT-LM generative runtime

## Status

Accepted

## Context

ASTRA currently has a LiteRT Android integration that can load a `.tflite` asset and execute `Interpreter.run(...)`. This proves the low-level local runtime path and keeps the architecture decoupled through `InferenceEngine`.

However, `Interpreter.run(...)` alone does not provide complete SLM text generation. A generative SLM also needs tokenization, prompt encoding, iterative token generation, sampling, stop-token handling, output decoding and model-specific configuration.

## Decision

ASTRA will keep two separate concepts:

1. **LiteRT tensor runtime**
   * Low-level backend.
   * Validates local `.tflite` model loading and tensor execution.
   * Useful for smoke tests, numeric models and future benchmark primitives.

2. **LiteRT-LM generative runtime**
   * Higher-level SLM backend.
   * Owns tokenizer/model bundle validation and the future generation session.
   * Exposed behind `InferenceEngine` as `LiteRtLmInferenceEngine`.

Presentation continues to depend only on MVI state and domain/core abstractions. It does not import LiteRT or LiteRT-LM APIs.

## Consequences

* LiteRT remains useful and simple.
* LiteRT-LM can evolve without UI architecture changes.
* Mock fallback remains the safety net.
* Android can expose LiteRT-LM as `Model Required` until a compatible bundle is added.
* iOS remains stable with LiteRT-LM marked unsupported for now.

## Alternatives considered

* Extend the tensor runtime into an SLM generator: rejected because tokenizer/sampling logic would blur responsibilities.
* Replace LiteRT with LiteRT-LM entirely: rejected because the tensor runtime remains valuable for lower-level validation.
* Implement a tokenizer from scratch: rejected for this task because it would add model-specific complexity and risk.

