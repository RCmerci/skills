# Logseq Datascript Query Examples

These examples focus on DB graph attributes. Use `:block/title` for block content in DB graph.

## Usage pattern (CLI)
- Return entity IDs directly (the entity var is the db/id).
- Fetch full content with `logseq show --id <id>`.
- If `show` doesn’t provide enough fields, extend the query to `pull` the extra attrs you need.

Example:
```bash
logseq query --repo "my-graph" \
  --query "[:find ?e :where [?e :block/name ?name]]" \
  --inputs "[\"home\"]" --output edn
logseq show --repo "my-graph" --id 123
```

## 1) Page lookup by name
```clojure
[:find ?p
 :in $ ?page-name
 :where
 [?p :block/name ?page-name]]
```

## 2) Blocks on a page
```clojure
[:find ?b
 :in $ ?page-name
 :where
 [?p :block/name ?page-name]
 [?b :block/page ?p]]
```

## 3) Child blocks of a parent block UUID
```clojure
[:find ?b
 :in $ ?parent-uuid
 :where
 [?parent :block/uuid ?parent-uuid]
 [?b :block/parent ?parent]]
```

## 4) Backlinks: blocks that reference a page
```clojure
[:find ?b
 :in $ ?page-name
 :where
 [?p :block/name ?page-name]
 [?b :block/refs ?p]]
```

## 5) Tagged blocks (tags are pages)
```clojure
[:find ?b
 :in $ ?tag-name
 :where
 [?tag :block/name ?tag-name]
 [?b :block/tags ?tag]]
```

## 6) Pages that list an alias
```clojure
[:find ?p
 :in $ ?alias-name
 :where
 [?alias :block/name ?alias-name]
 [?p :block/alias ?alias]]
```

## 7) Journal pages in a date range (YYYYMMDD)
```clojure
[:find ?p
 :in $ ?start ?end
 :where
 [?p :block/journal-day ?day]
 [(<= ?start ?day)]
 [(<= ?day ?end)]]
```

## 8) Recently updated blocks (timestamp range)
```clojure
[:find ?b
 :in $ ?start-ts ?end-ts
 :where
 [?b :block/updated-at ?ts]
 [(<= ?start-ts ?ts)]
 [(<= ?ts ?end-ts)]]
```

## 9) Files by path (file entities)
```clojure
[:find ?f
 :in $ ?path
 :where
 [?f :file/path ?path]]
```

## 10) Blocks linking to a class/tag page (DB graph)
```clojure
[:find ?b
 :in $ ?class-name
 :where
 [?class :block/name ?class-name]
 [?b :block/link ?class]]
```

## Notes
- Datalog results are unordered. If you need ordering, add `?order` and sort client-side.
- Prefer `:block/title` in DB graph (avoid `:block/content`).
