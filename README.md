# Skills Repository

This repository contains reusable Codex skills and utilities for working with ClojureDocs → Logseq imports.

## Repository layout

- `skills/` — all skill packages (`<skill-name>/SKILL.md`)
- `src/` — Clojure source code
- `test/` — tests
- `docs/agent-guide/` — planning and implementation documents

## Requirements

- [Babashka (`bb`)](https://babashka.org/)

## Common commands

- `bb tasks` — list available tasks
- `bb test` — run all tests
- `bb run link-skills` — link repo skills into destination roots
- `bb run unlink-skills` — remove repo-owned skill symlinks from destination roots

## Skill link configuration

`link-skills` and `unlink-skills` support these environment variables:

- `SKILLS_DEST_ROOTS` — destination roots separated by your OS path separator (`:` on macOS/Linux)
- `SKILLS_SOURCE_ROOT` — optional source root override (defaults to `<repo>/skills`)

Default destination roots:

- `~/.codex/skills`
- `~/.config/opencode/skills`
