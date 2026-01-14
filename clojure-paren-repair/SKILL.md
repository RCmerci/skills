---
name: clojure-paren-repair
description: Repair unbalanced parentheses, brackets, and braces in Clojure, ClojureScript, and EDN files. Use when you encounter delimiter mismatch syntax errors after editing .clj, .cljs, .cljc, or .edn files, or on clojure syntax errors.
---

# Clojure Parenthesis Repair (how to fix unbalanced brackets/parens)

Use `brepl balance` to fix unbalanced brackets in Clojure files using parmezan:

```bash
# Show help info
clj-paren-repair -h

# Fix file in place (default)
clj-paren-repair src/myapp/core.clj
```

This is useful for recovering files with bracket errors.
## When to Use

Run this tool when you encounter unbalanced delimiters (parentheses, brackets, braces) in Clojure, ClojureScript, or EDN files.

**IMPORTANT:** Do NOT try to manually repair parenthesis errors. If you encounter a file with unbalanced parentheses or delimiters, run `clj-paren-repair` on that file instead of attempting to fix the delimiters yourself.

## If the Tool Fails

If clj-paren-repair doesn't fix the problem, report to the user that they need to fix the delimiter error manually. Do not continue attempting repairs.
