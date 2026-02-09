---
name: logseq-cli
description: Operate the Logseq command-line interface to inspect or modify graphs, pages, blocks, tags, and properties; run Datascript queries (including query list and named queries); show page/block trees; manage graphs; and manage db-worker-node servers. Use when a request involves running `logseq` commands or interpreting CLI output.
---

# Logseq CLI

## Overview

Use the `logseq` CLI to query or edit a graph, manage graphs, and control servers.

## Glossary
- `db/id`: Internal numeric entity id (use with CLI flags like `--id`).
- `:block/uuid`: Stable UUID for a block entity; prefer when you need a persistent reference.
- `:block/name`: Lowercased page name, used for page lookup and joins.
- `:block/title`: Block or page title stored in the DB graph (use in queries when content text is needed).
- `:block/tags`: Ref-many attribute linking blocks to tag/page entities.

## Quick start

- Run `logseq --help` to see top-level commands and global flags.
- Run `logseq <command> --help` to see command-specific options.
- Use `--repo` to target a specific graph.
- Omit `--output` for human output. Set `--output` to `json` or `edn` only when machine-readable output is explicitly required.

## Command groups (from `logseq --help`)

- Graph inspect/edit: `list page|tag|property`, `add block|page`, `update`, `remove`, `query`, `query list`, `show`
- Graph management: `graph list|create|switch|remove|validate|info|export|import`
- Server management: `server list|status|start|stop|restart`

## Important notes
- If you are unsure about Datascript query syntax, use the `logseq-schema` skill for guidance.
- Never use following block attrs in `query` or `pull`, these attrs are file-graph only, never used in db-graphs:
`:block/format`, `:block/level`, `:block/level-spaces`, `:block/pre-block?`, `:block/properties-order`, `:block/properties-text-values`, `:block/invalid-properties`, `:block/macros`, `:block/file`, `:block.temp/ast-body`, `:block.temp/ast-blocks`, `:block/marker`, `:block/content`, `:block/priority`, `:block/scheduled`, `:block/deadline`, `:block/properties`, `:block/left`.

## Datascript Query Mistakes To Avoid
- In `query` `:where`/`pull`/`find`, attributes cannot use wildcards (e.g., `:logseq.property/*`); you must use full attr `:db/ident` values (e.g., `:logseq.property/status`, `:user.property/background`).
- Do not query `:db/id` as a normal datom attribute in `:where` (e.g., `[?e :db/id ?id]`); this is unreliable. Bind entities by stable attrs first (such as `:block/uuid` or `:block/title`), then `pull` `:db/id` if needed.
- Predicate clauses require bound variables. Avoid clauses like `[(= ?e ?id)]` before `?e` is bound, or Datascript may throw `Insufficient bindings`.

## Add: tags and properties

`add block` and `add page` support:
- `--tags` as an EDN vector. Each tag can be a tag id, `:db/ident` keyword, page title string, or UUID string.
- `--properties` as an EDN map. Keys must reference built-in properties using a property id, `:db/ident` keyword, or property title string. Values are EDN (string/number/boolean/keyword/uuid/map/vector).

Notes:
- `--tags` and `--properties` cannot be combined with `--blocks` or `--blocks-file`.
- If you need tag/property IDs or built-in property names, use `logseq list tag` or `logseq list property`.

## Examples

```bash
# List pages in a graph (human output by default)
logseq list page --repo "my-graph"
# List pages with pagination or sorting
logseq list page --repo "my-graph" --limit 50 --offset 0 --sort updated-at --order desc

# Run a Datascript query by name
logseq query --repo "my-graph" --name "recent-updated" --inputs "[30]"
# Run an ad-hoc Datascript query (EDN) returning db/id vector
logseq query --repo "my-graph" --query "[:find [?p ...] :where [?p :block/name]]"
# List available queries
logseq query list --repo "my-graph"

# Create a page and add a block to it
logseq add page --repo "my-graph" --page "Meeting Notes"
logseq add block --repo "my-graph" --target-page-name "Meeting Notes" --content "Discussed roadmap"

# Add tags to a block (tag values can be page titles, :db/ident, ids, or UUIDs)
logseq add block --repo "my-graph" --target-page-name "Meeting Notes" --content "Follow up" \
  --tags "[\"Task\" :logseq.class/Task]"

# Query all available status values
logseq query -name list-status --repo "my-graph"

# Add built-in properties to a block
logseq add block --repo "my-graph" --target-page-name "Meeting Notes" --content "Ship v1" \
  --properties "{:logseq.property/priority 2 :logseq.property/status :logseq.property/status.todo}"

# Add tags/properties when creating a page
logseq add page --repo "my-graph" --page "Project X" \
  --tags "[\"Task\"]" \
  --properties "{:logseq.property/description \"Example project\"}"

# Add multiple blocks via EDN (inline or file)
logseq add block --repo "my-graph" --target-page-name "Meeting Notes" --blocks "[{:block/title \"A\"} {:block/title \"B\"}]"
logseq add block --repo "my-graph" --target-page-name "Meeting Notes" --blocks-file "/tmp/blocks.edn"

# Move a block under another block (use update)
logseq update --repo "my-graph" --id <BLOCK_ID> --target-id <PARENT_BLOCK_ID> --pos last-child

# Remove a page or block
logseq remove --repo "my-graph" --uuid <BLOCK_UUID>
logseq add page --repo "my-graph" --page "Old Page"
logseq remove --repo "my-graph" --page "Old Page"

# Show a page tree (text) or a block (human by default)
logseq show --repo "my-graph" --page "Meeting Notes" --level 2
logseq show --repo "my-graph" --id <BLOCK_ID>
# Show multiple blocks in one command
logseq show --repo "my-graph" --id '[123,456,789]'

# Safely resolve an entity and include db/id in output
logseq query --repo "my-graph" --output edn \
  --query "[:find (pull ?e [:db/id :block/uuid :block/title]) . :in $ ?u :where [?e :block/uuid ?u]]" \
  --inputs "[#uuid \"11111111-1111-1111-1111-111111111111\"]"

# Create a graph, list graphs, switch the new created graph, get graph info
logseq graph create --repo "my-graph"
logseq graph list
logseq graph switch --repo "my-graph"
logseq graph info --repo "my-graph"

# Export/import a graph (`--output` is the destination file path)
logseq graph export --repo "my-graph" --type edn --output /tmp/my-graph.edn
logseq graph import --repo "my-graph-import" --type edn --input /tmp/my-graph.edn
```

## Tips

- Default to human output by omitting `--output`. Only set `--output edn` or `--output json` when machine-readable output is explicitly required.
- `--output` controls output format (human/json/edn). For `graph export`, `--output` is the destination file path.
- `show` uses global `--output` and accepts `--page`, `--uuid`, or `--id`, plus `--level` for depth.
- Use `--id` (block db/id) for `show` and `update`; use `--uuid` for `remove` when deleting a block.
- If you only know `db/id`, prefer `logseq show --id <id>` to inspect directly; if you are writing queries, bind by `:block/uuid`/`:block/title` and return `:db/id` via `pull`.
- When showing multiple blocks, pass them in one command as `--id '[id1,id2,id3...]'` rather than multiple `logseq show` calls.
- IDs shown in `list`/`show` output can be used with `show --id`.
- When adding long text, split it into multiple blocks and add them separately instead of putting a large paragraph into a single block.
- For `--blocks`/`--blocks-file`, use an EDN vector of block maps like `{:block/title "A"}`.
- `update` can move blocks via `--target-id` + `--pos`, and also add/remove tags and properties in the same command.
- Always confirm command flags with `logseq <command> --help`, since options vary by command.
- If `logseq` reports that it doesn’t have read/write permission for data-dir, then add read/write permission for data-dir in the agent’s config.

## References

- Built-in tags and properties: See `references/logseq-builtins.md` when you need the canonical list of Logseq built-in tags (classes) or properties for `--tags`/`--properties` arguments.
