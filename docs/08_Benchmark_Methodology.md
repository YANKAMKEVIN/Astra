# Benchmark Methodology

## What ASTRA measures

The Benchmark Lab now routes benchmark execution through the same inference architecture as the Assistant:

```text
Benchmark
  ↓
PromptPipeline
  ↓
RoutingInferenceEngine
  ↓
Selected runtime
  ↓
GenerationResult
  ↓
TaskEvaluationEngine
```

Each result reports:

* selected backend;
* actual runtime used;
* runtime mode;
* model load time when available;
* inference latency when available;
* total execution time when available;
* time to first token when available;
* tokens/sec when available;
* memory usage when available;
* fallback reason when fallback occurs;
* Task Evaluation Score.

Unavailable metrics are shown as `N/A`.

## What is real

When a runtime can execute locally, ASTRA uses metrics returned by `GenerationResult` from the selected inference path.

For LiteRT tensor validation, this means metrics from the Android LiteRT `Interpreter.run(...)` path when a compatible model asset exists.

## What is simulated

`MockInferenceEngine` remains a deterministic local fallback and demo engine. When the mock engine is used, the runtime mode indicates simulated local inference or mock fallback so engineers can distinguish it from real runtime execution.

## Fallback representation

Fallback is explicit. Benchmark results show:

```text
Selected: <requested backend>
Used: <actual backend>
Reason: <fallback reason>
```

Examples:

* selected LiteRT-LM, used Mock Engine, model files missing;
* selected LiteRT, used Mock Engine, `.tflite` asset missing;
* selected unsupported runtime, used Mock Engine.

## Recommendation logic

Recommendations are computed with the following priority:

1. higher Task Evaluation Score;
2. real runtime availability over mock/fallback when scores are close;
3. lower total execution time when available;
4. lower memory usage when available.

This keeps the recommendation business-task oriented while still rewarding real local execution and efficient runtime behavior.

## Limitations

* Battery and thermal metrics are not collected.
* Benchmark history is not persisted.
* Real LiteRT-LM generation is not implemented yet.
* Missing runtime metrics are intentionally displayed as `N/A` rather than invented.

