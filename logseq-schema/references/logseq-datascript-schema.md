# Logseq Datascript Schema Reference

## Core schema attributes

Attributes and notes:
- `:db/ident` (unique identity)
- `:kv/value`
- `:block/uuid` (unique identity)
- `:block/parent` (ref, indexed)
- `:block/order` (indexed)
- `:block/collapsed?`
- `:block/page` (ref, indexed)
- `:block/refs` (ref, many)
- `:block/tags` (ref, many)
- `:block/link` (ref, indexed)
- `:block/alias` (ref, many, indexed)
- `:block/created-at` (indexed)
- `:block/updated-at` (indexed)
- `:block/name` (indexed, lowercased page name)
- `:block/title` (indexed)
- `:block/journal-day` (indexed)
- `:block/tx-id`
- `:block/closed-value-property` (ref, many)
- `:file/path` (unique identity)
- `:file/content`
- `:file/created-at`
- `:file/last-modified-at`
- `:file/size`

## Built-in properties

Notes:
- Keys are property idents and may include block-level attributes.
- Each entry includes a config map with `:schema` (type/cardinality/public?/view-context), optional `:attribute` (datascript attribute outside `:block/properties`), and other metadata.
- Use this map to determine whether a property is public/queryable and how to interpret its value.

## Built-in classes

Notes:
- Keys are class idents.
- Values define `:title`, optional `:properties`, and optional `:schema` (including `:properties` and `:required-properties`).
- Use this map for tag/class definitions and required properties when crafting queries.

## Discover `:db/ident` entities via CLI

If you need a fresh list of entities with `:db/ident`, use the CLI to create a temporary graph and query:

```bash
logseq graph create --repo "schema-inspect"
logseq query --repo "schema-inspect" --output edn \
  --query "[:find ?ident :where [_ :db/ident ?ident]]"
```

Use the result set to confirm what is in the graph and to spot new schema entities.
