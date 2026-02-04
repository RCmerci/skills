---
name: clojure-debug
description: Debugging workflow for Clojure/ClojureScript code. Use at the first sign of unexpected behavior or test failure in Clojure/CLJS, including any failing test, unexpected output, nils where data is expected, mismatched selectors, or unclear data flow—before making further implementation changes.
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
