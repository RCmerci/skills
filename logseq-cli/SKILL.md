---
name: logseq-cli
description: Operate the Logseq command-line interface to inspect or modify graphs, pages, blocks, tags, and properties; run searches; show page trees; manage graphs; and manage db-worker-node servers. Use when a request involves running `logseq` commands, interpreting CLI output.
---

# Logseq CLI

## Overview

Use the `logseq` CLI to query or edit a graph, manage graphs, and control db-worker-node servers. Prefer default output (`human`) unless the result needs to be parsed; use `json`/`edn` for automation.

## Quick start

- Run `logseq --help` to see top-level commands and global flags.
- Run `logseq <command> --help` to see command-specific options.
- Use `--repo` to target a specific graph and `--data-dir` when the db-worker data directory is non-default.
- Use `--config` to point at a custom `cli.edn` and `--timeout-ms` to tune request timeouts.
- Set `--output` to `json` or `edn` when you need machine-readable output.

## Command groups (from `logseq --help`)

- Graph inspect/edit: `list page|tag|property`, `add block|page`, `move`, `remove block|page`, `query`, `query list`, `show`
- Graph management: `graph list|create|switch|remove|validate|info|export|import`
- Server management: `server list|status|start|stop|restart`

## Examples

```bash
# List pages in a graph, JSON output for scripting
logseq list page --repo "my-graph" --output json
# List pages in a graph, human readable output (lower token usage)
logseq list page --repo "my-graph"
# Filter pages and select fields
logseq list page --repo "my-graph" --updated-after 2024-01-01T00:00:00Z --fields name,updated-at --limit 50

# Run a Datascript query by name (from cli.edn or built-ins)
logseq query --repo "my-graph" --name "recently-updated" --inputs "[\"2024-01-01\"]"
# Run an ad-hoc Datascript query (EDN)
logseq query --repo "my-graph" --query "[:find (pull ?p [*]) :where [?p :block/name]]"
# List available queries
logseq query list --repo "my-graph"

# Create a page and add a block to it
logseq add page --repo "my-graph" --page "Meeting Notes"
logseq add block --repo "my-graph" --target-page-name "Meeting Notes" --content "Discussed roadmap"
# Add multiple blocks via EDN
logseq add block --repo "my-graph" --target-page-name "Meeting Notes" --blocks "[{:content \"A\"} {:content \"B\"}]"

# Move a block under another block
logseq move --repo "my-graph" --id <BLOCK_ID> --target-id <PARENT_BLOCK_ID> --pos last-child

# Remove a page or block
logseq remove page --repo "my-graph" --page "Old Page"
logseq remove block --repo "my-graph" --block <BLOCK_UUID>

# Show a page tree (text) or a block (json)
logseq show --repo "my-graph" --page-name "Meeting Notes"
logseq show --repo "my-graph" --id <BLOCK_ID> --format json

# Create a graph, list graphs, switch the new created graph, get graph info
logseq graph create --repo "my-graph"
logseq graph list
logseq graph switch --repo "my-graph"
logseq graph info --repo "my-graph"

# Export/import a graph
logseq graph export --repo "my-graph" --type edn
logseq graph import --repo "my-graph" --type edn --input /path/to/graph.edn
```

## Tips

- Prefer default output (`human`) over `json` when possible to reduce token usage.
- `show` uses `--format` (text/json/edn); other commands use `--output` (human/json/edn).
- Use `--id` (block db/id) for `show` and `move`; `remove block` expects a UUID via `--block`.
- IDs shown in `list`/`show` output can be used with `show --id`.
- Always confirm command flags with `logseq <command> --help`, since options vary by command.
