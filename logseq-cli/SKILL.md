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
- Use `--repo` to target a specific graph and `--data-dir` when the server data directory is non-default.

## Command groups (from `logseq --help`)

- Graph inspect/edit: `list page|tag|property`, `add block|page`, `move`, `remove block|page`, `search`, `show`
- Graph management: `graph list|create|switch|remove|validate|info|export|import`
- Server management: `server list|status|start|stop|restart`

## Examples

```bash
# List pages in a graph, JSON output for scripting
logseq list page --repo "my-graph" --output json
# List pages in a graph, human readable output (lower token usage)
logseq list page --repo "my-graph"

# Search a graph for a query string
logseq --repo "my-graph" search "project alpha"

# Create a page and add a block to it
logseq add page --repo "my-graph" --page "Meeting Notes"
logseq add block --repo "my-graph" --target-page-name "Meeting Notes" --content "Discussed roadmap"

# Move a block under another block
logseq move --repo "my-graph" --id "<BLOCK_ID>" --target-id "<PARENT_BLOCK_ID>" --pos last-child

# Remove a page
logseq remove page --repo "my-graph" --page "Old Page"

# Show a page tree
logseq show --repo "my-graph" --page-name "Meeting Notes"

# Show a block by ID
logseq show --repo "my-graph" --id <BLOCK_ID>

# Create a graph, list graphs, switch the new created graph, get graph info
logseq graph create --repo "my-graph"
logseq graph list
logseq graph switch --repo "my-graph"
logseq graph info --repo "my-graph"
```

## Tips

- Prefer default output (`human`) over `json` when possible to reduce token usage.
- Prefer `--id` (block ID) instead of `--uuid` when operating on blocks.
- IDs shown in `list`/`show` output can be used with `show --id`.
- Always confirm command flags with `logseq <command> --help`, since options vary by command.
