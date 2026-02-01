---
name: clojure-debug
description: Debugging workflow for Clojure/ClojureScript code. Use when investigating failing tests, unexpected behavior, or unclear data flow in Clojure/CLJS. Emphasize early insertion of `prn` in the execution path and validating small hypotheses with `clojure/node` CLI `eval`.
---

# Clojure Debug

## Core workflow

- Insert `prn` (or `prn` + labels) as early as possible in the suspected execution path.
- Prefer multiple small `prn` checkpoints over one large dump.
- Keep `prn` output focused on inputs, branch decisions, and key transforms.
- Remove or guard debug prints after validation.

## `clojure/node` CLI validation

- Use `clojure/node` CLI `eval` to validate small snippets or hypotheses.
- Keep eval expressions minimal and deterministic.
- Use it to confirm parsing, coercion, and data-shape assumptions before editing more code.

## Tips

- Favor `prn` over `println` to preserve readable EDN.
- When debugging async flows, print at boundaries: before request, after response, before transform, after transform.
- Add labels to output to correlate logs with branches.
