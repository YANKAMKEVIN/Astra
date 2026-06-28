# Task Evaluation Methodology

## What the score represents

ASTRA's Task Evaluation Score estimates how well a generated response satisfies the selected operational task.

It is designed for engineers, architects and business stakeholders who need a transparent local signal during demos and model comparisons.

The score is:

* local;
* deterministic;
* rule-based;
* explainable;
* scoped to operational task usefulness.

It is not a claim of model intelligence, safety certification or academic benchmark performance.

## Why this is not an academic benchmark

ASTRA does not use MMLU, HumanEval, semantic embeddings, cloud judges or LLM-as-a-judge in this sprint.

The current methodology evaluates business-task fit using simple rules that are easy to inspect and tune. This makes the Benchmark Lab more credible than an arbitrary quality number while keeping the app offline-first.

## Criteria and weights

| Criterion | Weight | What it checks |
|---|---:|---|
| Safety | 30 | Safety-first reasoning, warnings, risk checks and safe operating conditions. |
| Procedure Completeness | 25 | Whether the answer covers expected operational steps in a structured way. |
| Technical Accuracy | 20 | Technically plausible, non-dangerous operational checks and instructions. |
| Domain Terminology | 15 | Use of vocabulary relevant to the selected industry. |
| Clarity | 10 | Readability, structure and actionability. |

Total weight: 100.

## Current implementation

`RuleBasedTaskEvaluationEngine` evaluates:

* safety keywords such as `verify`, `inspect`, `isolate`, `warning`, `emergency`, `pressure`, `shutdown`;
* structured steps and procedural wording;
* technical checks and unsafe instruction penalties;
* industry-specific terminology;
* response clarity and structure.

Each criterion produces a score from 0 to 100. Weighted criterion scores are combined into an overall score from 0 to 100.

## Limitations

* Keyword rules can miss good answers that use different wording.
* The engine does not understand deeper semantics yet.
* It does not validate against real manuals or certified procedures.
* It should not be used as a safety approval mechanism.
* It does not persist evaluation history.

## Future improvements

Potential future iterations:

* task-specific expected-step checklists;
* industry-specific evaluation profiles;
* optional local embedding similarity;
* human review annotations;
* benchmark datasets stored locally;
* comparison against approved maintenance procedures.

