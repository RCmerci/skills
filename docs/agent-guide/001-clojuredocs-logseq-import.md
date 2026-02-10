# ClojureDocs JSON to Logseq Graph Implementation Plan

Goal: Convert the ClojureDocs export JSON into importable Logseq EDN and validate a `clojure-docs-*` graph for Codex query workflows.

Architecture: A babashka CLI fetches and normalizes ClojureDocs JSON, maps vars and related records into page and block entities, and writes one Logseq graph export EDN file.
Architecture: The EDN explicitly defines `:pages-and-blocks`, `:classes`, and `:properties` so `logseq graph import` succeeds without manual fixes.
Architecture: Validation imports into an isolated graph and runs deterministic `logseq query` checks that mirror future Codex usage.

Tech Stack: Babashka, Clojure EDN, `logseq-cli`, Datascript query, curl.

Related: Relates to none yet in `docs/agent-guide/`.

## Problem statement

We need a repeatable way to convert `https://clojuredocs.org/clojuredocs-export.json` into a Logseq graph that can be queried by Codex when writing Logseq related code.

The output graph name must start with `clojure-docs`.

The EDN shape must favor query ergonomics over perfect source fidelity because the main consumer is CLI query automation.

The import format has strict constraints, including `:build/tags` and `:build/children` usage and explicit declaration of every user property in `:properties`.

This plan uses `@planning-documents`, `@writing-plans`, `@test-driven-development`, `@clojure-babashka-cli`, `@logseq-cli`, and `@logseq-schema`.

## Testing Plan

I will add integration tests that verify JSON input is transformed into import-safe EDN with required top-level keys and valid page and block build fields.

I will add behavior tests that assert tags are emitted under `:build/tags`, nested content is emitted under `:build/children`, and user properties are declared in `:properties` before any use.

I will add a command-level validation test that imports generated EDN into a temporary `clojure-docs-*` graph and executes query assertions against expected tags, pages, and metadata joins.

I will add regression tests for null or missing `examples`, `notes`, and `see-alsos` arrays so import output remains valid and deterministic.

I will add regression tests for value escaping and multi-line bodies so example and note text survives round trip without broken EDN.

NOTE: I will write *all* tests before I add any implementation behavior.

## Target EDN model

The graph file will follow the native Logseq export envelope and will preserve schema and graph file metadata from a freshly exported skeleton file.

The converter will fill only `:pages-and-blocks`, `:classes`, and `:properties`, then regenerate kv values for graph UUID and timestamps.

```edn
{:pages-and-blocks [...]
 :classes {...}
 :properties {...}
 :logseq.db.sqlite.export/schema-version {:major <from-skeleton> :minor <from-skeleton>}
 :logseq.db.sqlite.export/graph-files [...]
 :logseq.db.sqlite.export/kv-values [...]
 :logseq.db.sqlite.export/export-type :graph}
```

Entity mapping is defined below.

| Source object | Logseq entity | Tags | Key properties | Query reason |
| --- | --- | --- | --- | --- |
| var (`ns` + `name`) | one page | `:ClojureDocs.Var`, namespace class | `:source-id`, `:ns`, `:var-name`, `:type`, `:href`, `:library-url` | fast page lookup and namespace filtering |
| var doc text | one child block under `doc` section | `:ClojureDocs.Doc` | `:item-type` | keeps primary doc accessible via page tree |
| arglists | one section block + child block per arglist | `:ClojureDocs.Arglists` | `:item-type`, `:arg-index` | deterministic argument introspection |
| example | one child block per example | `:ClojureDocs.Example` | `:source-id`, `:created-at`, `:updated-at`, `:author-login` | filter and inspect examples independently |
| note | one child block per note | `:ClojureDocs.Note` | `:source-id`, `:created-at`, `:updated-at`, `:author-login` | independent note query path |
| see-also link | one child block per edge | `:ClojureDocs.SeeAlso` | `:target-ns`, `:target-name`, `:source-id` | graph navigation and relationship query |
| import metadata | one page | `:ClojureDocs.Metadata` | `:source-url`, `:source-created-at`, `:import-created-at`, `:vars-count` | reproducibility and auditability |

Namespace tags will be normalized to class keywords like `:ClojureDocs.Ns.clojure.core`.

Important searchable identifiers will be duplicated in block titles to reduce query join complexity on value entities.

## Query-first conventions

Every var page title will be `<ns>/<name>` such as `clojure.core/map`.

Section block titles will be fixed and lowercase, including `doc`, `arglists`, `examples`, `notes`, and `see-alsos`.

Each see-also block title will start with `see-also: <target-ns>/<target-name>` to support title-only fallback queries.

Each example block title will start with `example: <source-id>` so users can find a specific snippet without property joins.

## Integration sketch

```text
clojuredocs-export.json
        |
        v
normalize + validate + enrich
        |
        v
build {:classes :properties :pages-and-blocks}
        |
        v
merge into skeleton export envelope
        |
        v
clojure-docs-*.edn
        |
        v
logseq graph import --repo clojure-docs-*
        |
        v
logseq query / show validation
```

## Implementation tasks

1. Create `/Users/rcmerci/gh-repos/skills/bb.edn` with tasks for `test`, `convert`, and `validate-import`.
2. Create `/Users/rcmerci/gh-repos/skills/src/clojuredocs_to_logseq/cli.clj` and define subcommands with `babashka.cli`.
3. Create `/Users/rcmerci/gh-repos/skills/src/clojuredocs_to_logseq/fetch.clj` to fetch source JSON and read local fixtures.
4. Create `/Users/rcmerci/gh-repos/skills/src/clojuredocs_to_logseq/model.clj` for normalized intermediate records.
5. Create `/Users/rcmerci/gh-repos/skills/src/clojuredocs_to_logseq/transform.clj` for JSON to normalized model conversion.
6. Create `/Users/rcmerci/gh-repos/skills/src/clojuredocs_to_logseq/logseq_edn.clj` for normalized model to import EDN conversion.
7. Create `/Users/rcmerci/gh-repos/skills/src/clojuredocs_to_logseq/skeleton.clj` to load and patch envelope keys from an exported skeleton file.
8. Create `/Users/rcmerci/gh-repos/skills/test/fixtures/clojuredocs/sample.json` with one var containing examples, notes, and see-alsos.
9. Create `/Users/rcmerci/gh-repos/skills/test/fixtures/logseq/skeleton-export.edn` from an actual `logseq graph export` run.
10. Write failing test in `/Users/rcmerci/gh-repos/skills/test/clojuredocs_to_logseq/transform_test.clj` for var page title and section block generation.
11. Run `bb test --focus clojuredocs-to-logseq.transform-test/var-page-mapping` and confirm RED failure is behavior related.
12. Write failing test in `/Users/rcmerci/gh-repos/skills/test/clojuredocs_to_logseq/transform_test.clj` for `examples` mapping to `:build/children`.
13. Run focused test and confirm failure because children are absent or malformed.
14. Write failing test in `/Users/rcmerci/gh-repos/skills/test/clojuredocs_to_logseq/transform_test.clj` for user properties declaration completeness.
15. Run focused test and confirm failure reports missing property declarations.
16. Write failing test in `/Users/rcmerci/gh-repos/skills/test/clojuredocs_to_logseq/transform_test.clj` for null `notes` and null `see-alsos` handling.
17. Run focused test and confirm failure is missing optional section behavior.
18. Write minimal implementation in `transform.clj` for core var page and section mapping.
19. Run focused tests and confirm GREEN for already written RED tests.
20. Refactor mapping helpers for repeated block and property generation while keeping tests green.
21. Write failing test in `/Users/rcmerci/gh-repos/skills/test/clojuredocs_to_logseq/logseq_edn_test.clj` for envelope merge behavior with skeleton export keys.
22. Run focused test and confirm RED on missing required top-level keys.
23. Implement minimal envelope merge in `logseq_edn.clj` and `skeleton.clj`.
24. Run focused tests and confirm GREEN.
25. Write failing test in `/Users/rcmerci/gh-repos/skills/test/clojuredocs_to_logseq/import_validation_test.clj` for `logseq graph import` success on generated output.
26. Run import test and confirm RED when generated file is not yet import safe.
27. Implement minimal CLI wiring in `cli.clj` and add `validate-import` command that imports into a temp repo.
28. Run full test suite with `bb test` and confirm all tests pass.
29. Run `bb run convert --input-url https://clojuredocs.org/clojuredocs-export.json --output /tmp/clojure-docs.edn --graph-name clojure-docs-$(date +%Y%m%d)-v1`.
30. Run `logseq --data-dir /tmp/logseq-cli-plan graph import --repo clojure-docs-$(date +%Y%m%d)-v1 --type edn --input /tmp/clojure-docs.edn`.
31. Run query validation command for var page counts by class tag and confirm non-zero result.
32. Run query validation command for one known var page and confirm `ns` and `var-name` values are resolvable.
33. Run query validation command for see-also edges and confirm at least one result set.
34. Export imported graph to `/tmp/clojure-docs-roundtrip.edn` and confirm output parses with `clojure.edn/read-string`.
35. Capture validation summary into `/Users/rcmerci/gh-repos/skills/docs/agent-guide/001-clojuredocs-logseq-import.md` appendix once implementation starts.

## Logseq CLI validation commands

Use an isolated data dir so validation is reproducible and does not affect local default graphs.

```bash
export DATA_DIR=/tmp/logseq-cli-plan
export REPO=clojure-docs-$(date +%Y%m%d)-v1
logseq --data-dir "$DATA_DIR" graph remove --repo "$REPO" || true
logseq --data-dir "$DATA_DIR" graph import --repo "$REPO" --type edn --input /tmp/clojure-docs.edn
logseq --data-dir "$DATA_DIR" query --repo "$REPO" --output edn \
  --query "[:find (count ?p) . :where [?p :block/tags :user.class/ClojureDocsVar]]"
logseq --data-dir "$DATA_DIR" show --repo "$REPO" --page "clojure.core/map" --output edn
```

Expected behavior is `Imported edn from ...`, a positive var page count, and a non-empty page tree for at least one known var.

## Edge cases

The source JSON may contain null arrays for `notes`, `examples`, or `see-alsos`, and conversion must emit empty sections without invalid nil children.

Large example bodies may contain fenced code, escaped characters, and long lines, so conversion must preserve exact text and EDN escaping.

`see-alsos` may reference vars outside the imported set, so importer must keep edge metadata even when target page is absent.

Duplicate `name` values across namespaces must never collide because page identity uses `<ns>/<name>`.

Graph schema version may change across Logseq releases, so the converter must derive envelope metadata from a fresh skeleton export instead of hardcoding.

## Acceptance criteria

Generated EDN imports successfully with `logseq graph import` into a repo named `clojure-docs-*`.

At least one query by class tag, one query by page title, and one query by see-also block returns expected non-empty data.

Round-trip export from imported graph remains parseable EDN and retains var pages and section block structure.

Metadata page records source URL and source export timestamp for provenance.

## Testing Details

Tests will verify observable behavior, including import success, queryability, and stable mapping semantics, instead of testing internal helper implementation details.

Import validation tests will execute real `logseq` CLI commands against a temporary graph and assert command exit status and key query outputs.

Property behavior tests will verify both declaration and resolution behavior by querying through value entities where required.

## Implementation Details
- Use `:build/tags` and `:build/children` in import EDN because `:block/tags` and `:block/children` are not accepted as build inputs.
- Declare all user properties in `:properties` before any page or block uses them.
- Keep graph envelope keys from a real skeleton export to avoid schema drift failures.
- Use deterministic section titles and title prefixes to simplify fallback queries.
- Keep main searchable identifiers in block titles even when also present as properties.
- Keep graph name format as `clojure-docs-<yyyyMMdd>-v1` for deterministic cleanup and reruns.
- Use a dedicated temp data dir for validation to avoid mutating default local graphs.
- Run RED and GREEN checkpoints with focused tests before each mapping implementation step.
- Treat missing optional arrays as empty collections and never emit nil child blocks.
- Verify final output with both import and post-import query checks.

## Question

Should the first version import all namespaces in the export file, or only `clojure.core` and closely related built-in namespaces such as `clojure.set`, `clojure.string`, and `clojure.walk`.

Answer: Yes, import all namespaces in the export file for v1.

---

## Validation Appendix (2026-02-10)

Implementation validation was executed with:

- `DATA_DIR=/tmp/logseq-cli-plan`
- `REPO=clojure-docs-20260210-v1`
- Skeleton export: `/tmp/skeleton-export.edn`
- Generated import file: `/tmp/clojure-docs.edn`

Commands executed:

```bash
bb run convert \
  --input-url https://clojuredocs.org/clojuredocs-export.json \
  --skeleton /tmp/skeleton-export.edn \
  --output /tmp/clojure-docs.edn \
  --graph-name clojure-docs-20260210-v1

bb run validate-import \
  --input /tmp/clojure-docs.edn \
  --repo clojure-docs-20260210-v1 \
  --data-dir /tmp/logseq-cli-plan

logseq --timeout-ms 600000 --data-dir /tmp/logseq-cli-plan query \
  --repo clojure-docs-20260210-v1 --output edn \
  --query '[:find (count ?p) . :where [?p :block/tags :user.class/ClojureDocsVar]]'

logseq --timeout-ms 600000 --data-dir /tmp/logseq-cli-plan show \
  --repo clojure-docs-20260210-v1 --page 'clojure.core/map' --output edn

logseq --timeout-ms 600000 --data-dir /tmp/logseq-cli-plan query \
  --repo clojure-docs-20260210-v1 --output edn \
  --query '[:find (count ?b) . :where [?b :block/tags :user.class/ClojureDocsSeeAlso]]'

logseq --timeout-ms 600000 --data-dir /tmp/logseq-cli-plan graph export \
  --repo clojure-docs-20260210-v1 --type edn --output /tmp/clojure-docs-roundtrip.edn

bb -e '(require (quote [clojure.edn :as edn])) (edn/read-string (slurp "/tmp/clojure-docs-roundtrip.edn"))'
```

Observed results:

- Var class count query returned `{:status :ok, :data {:result 1550}}`
- See-also class count query returned `{:status :ok, :data {:result 1301}}`
- `show --page clojure.core/map` returned a non-empty page tree with sections and child blocks
- Round-trip export at `/tmp/clojure-docs-roundtrip.edn` parsed successfully with `clojure.edn/read-string`
