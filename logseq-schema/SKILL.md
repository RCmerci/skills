---
name: logseq-schema
description: Logseq Datascript schema, built-in properties/classes, and :db/ident discovery for composing or reviewing Datascript queries about blocks/pages/tags/properties/classes. Use when writing or validating Logseq Datascript queries or interpreting Logseq schema attributes.
---

# Logseq Schema

## Overview
Use this skill to ground Datascript queries in Logseq's schema: core block/page/file attributes, built-in properties, built-in classes, and schema entities with :db/ident. Load `references/logseq-datascript-schema.md` for authoritative sources and query patterns, and
`references/logseq-datascript-query-examples.md` for scenario-based query examples.

## Glossary
- `db/id`: Internal numeric entity id (use with CLI flags like `--id`).
- `:block/uuid`: Stable UUID for a block entity; prefer when you need a persistent reference.
- `:block/name`: Lowercased page name, used for page lookup and joins.
- `:block/title`: Block or page title stored in the DB graph (use in queries when content text is needed).
- `:block/tags`: Ref-many attribute linking blocks to tag/page entities.

## Workflow

### 1) Locate schema facts
- Open `references/logseq-datascript-schema.md`.
- Review the core attribute list and helper sets for ref/cardinality details.
- Review built-in properties and classes to understand available attributes and required fields.

### 2) Write or validate queries
- Prefer `:block/*` attributes for block/page queries; use properties/classes only when needed.
- If unsure about available `:db/ident` entities, run the CLI query listed in the references file.

### 3) Keep queries consistent with schema
- Respect ref vs scalar attributes and `:db.cardinality/many` when joining.
- Use property/class definitions to confirm public/queryable status before exposing a query to users.

## Resources

### references/
- `logseq-datascript-schema.md`
- `logseq-datascript-query-examples.md`
