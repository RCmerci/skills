# Malli API Reference

## Table of Contents
- [Core Validation (malli.core)](#core-validation-mallicore)
- [Transformation (malli.transform)](#transformation-mallitransform)
- [Error Handling (malli.error)](#error-handling-mallierror)
- [Utilities (malli.util)](#utilities-malliutil)

## Core Validation (malli.core)

Require as: `(require '[malli.core :as m])`

### Schema Creation

| Function | Signature | Purpose |
|----------|-----------|---------|
| `schema` | `([?schema] [?schema options])` | Create Schema from vector/keyword |
| `schema?` | `([x])` | Check if x is a Schema |

```clojure
(m/schema [:map [:x :int]])
(m/schema :string)
(m/schema [:vector {:min 1} :int])
```

### Validation

| Function | Signature | Purpose |
|----------|-----------|---------|
| `validate` | `([?schema value] [?schema value options])` | Returns true/false |
| `validator` | `([?schema] [?schema options])` | Returns cached `x -> boolean` fn |
| `explain` | `([?schema value] [?schema value options])` | Returns explanation or nil |
| `explainer` | `([?schema] [?schema options])` | Returns cached `x -> explanation` fn |

```clojure
;; Simple validation
(m/validate :int 42)           ;; => true
(m/validate :int "foo")        ;; => false

;; Cached validator (better perf)
(def valid? (m/validator [:map [:x :int]]))
(valid? {:x 1})                ;; => true

;; Get error details
(m/explain [:map [:x :int]] {:x "bad"})
;; => {:schema ..., :value {:x "bad"},
;;     :errors [{:path [:x], :in [:x], :schema :int, :value "bad"}]}
```

### Parsing

| Function | Signature | Purpose |
|----------|-----------|---------|
| `parse` | `([?schema value] [?schema value options])` | Parse value, returns `::m/invalid` on failure |
| `parser` | `([?schema] [?schema options])` | Returns cached parser fn |
| `unparse` | `([?schema value] [?schema value options])` | Inverse of parse |
| `unparser` | `([?schema] [?schema options])` | Returns cached unparser fn |

```clojure
;; Parse with :orn returns tagged result
(m/parse [:orn [:int :int] [:str :string]] 42)
;; => [:int 42]

(m/parse [:orn [:int :int] [:str :string]] "hello")
;; => [:str "hello"]

(m/parse :int "bad")
;; => :malli.core/invalid
```

### Schema Inspection

| Function | Signature | Purpose |
|----------|-----------|---------|
| `type` | `([?schema])` | Returns schema type keyword |
| `properties` | `([?schema])` | Returns properties map |
| `children` | `([?schema])` | Returns child schemas |
| `entries` | `([?schema])` | Returns map entries |
| `form` | `([?schema])` | Returns original schema form |

```clojure
(m/type [:map [:x :int]])      ;; => :map
(m/children [:map [:x :int]])  ;; => [[:x nil :int]]
(m/properties [:string {:min 1}]) ;; => {:min 1}
```

## Transformation (malli.transform)

Require as: `(require '[malli.transform :as mt])`

### Decode/Encode (in malli.core)

| Function | Signature | Purpose |
|----------|-----------|---------|
| `decode` | `([?schema value transformer])` | Transform value using decoder |
| `decoder` | `([?schema transformer])` | Returns cached decoder fn |
| `encode` | `([?schema value transformer])` | Transform value using encoder |
| `encoder` | `([?schema transformer])` | Returns cached encoder fn |
| `coerce` | `([?schema value transformer])` | Decode + validate, throws on error |
| `coercer` | `([?schema transformer])` | Returns cached coercer fn |

```clojure
;; Decode strings to proper types
(m/decode :int "42" (mt/string-transformer))
;; => 42

;; Decode doesn't validate! (returns bad data as-is)
(m/decode :int "not-a-number" (mt/string-transformer))
;; => "not-a-number"

;; Coerce = decode + validate (throws on invalid)
(m/coerce :int "42" (mt/string-transformer))
;; => 42

(m/coerce :int "bad" (mt/string-transformer))
;; => throws ExceptionInfo
```

### Built-in Transformers

| Transformer | Purpose |
|-------------|---------|
| `string-transformer` | Parse strings to types (int, boolean, uuid, etc.) |
| `json-transformer` | Handle JSON peculiarities (string keys, dates) |
| `default-value-transformer` | Apply `:default` property values |
| `strip-extra-keys-transformer` | Remove keys not in map schema |
| `collection-transformer` | Convert between collection types |

```clojure
;; String transformer - most common for web forms
(m/decode [:map [:port :int] [:active :boolean]]
          {:port "8080" :active "true"}
          (mt/string-transformer))
;; => {:port 8080, :active true}

;; JSON transformer - for JSON API responses
(m/decode [:map [:id :uuid] [:created :inst]]
          {:id "550e8400-e29b-41d4-a716-446655440000"
           :created "2024-01-15T10:30:00Z"}
          (mt/json-transformer))
;; => {:id #uuid "550e8400...", :created #inst "2024-01-15..."}

;; Default values
(m/decode [:map [:host [:string {:default "localhost"}]] [:port :int]]
          {:port 8080}
          (mt/default-value-transformer))
;; => {:host "localhost", :port 8080}

;; Strip unknown keys
(m/decode [:map {:closed true} [:x :int]]
          {:x 1 :y 2 :z 3}
          (mt/strip-extra-keys-transformer))
;; => {:x 1}
```

### Combining Transformers

```clojure
;; Chain multiple transformers
(m/decode schema value
          (mt/transformer
            (mt/default-value-transformer)
            (mt/string-transformer)
            (mt/strip-extra-keys-transformer)))
```

### Key Transformer

```clojure
;; Transform map keys (e.g., JSON camelCase to kebab-case)
(m/decode [:map [:user-name :string]]
          {"userName" "alice"}
          (mt/key-transformer {:decode keyword}))
```

### Custom Transformers

```clojure
(mt/transformer
  {:name "my-transformer"
   :decoders {:string {:compile (fn [schema _]
                                  (let [{:keys [trim]} (m/properties schema)]
                                    (when trim
                                      #(cond-> % (string? %) str/trim))))}}})
```

## Error Handling (malli.error)

Require as: `(require '[malli.error :as me])`

### Humanizing Errors

| Function | Signature | Purpose |
|----------|-----------|---------|
| `humanize` | `([explanation] [explanation opts])` | Convert explanation to readable form |
| `with-spell-checking` | `([explanation])` | Add typo suggestions |
| `error-value` | `([explanation] [explanation opts])` | Get just the invalid parts |

```clojure
;; Basic humanization
(-> [:map [:x :int] [:y :string]]
    (m/explain {:x "bad" :y 123})
    (me/humanize))
;; => {:x ["should be an integer"], :y ["should be a string"]}

;; With spell checking for typos
(-> [:map [:name :string]]
    (m/explain {:naem "alice"})
    (me/with-spell-checking)
    (me/humanize))
;; => {:naem ["should be spelled :name"]}

;; Get error values with context
(-> [:map [:x :int]]
    (m/explain {:x "bad"})
    (me/humanize {:wrap #(select-keys % [:value :message])}))
;; => {:x [{:value "bad", :message "should be an integer"}]}
```

### Custom Error Messages

```clojure
;; In schema
[:string {:min 3 :error/message "must be at least 3 characters"}]

;; Localized
[:int {:error/message {:en "must be integer" :fi "täytyy olla kokonaisluku"}}]

;; In humanize call
(me/humanize explanation {:locale :fi})
```

## Utilities (malli.util)

Require as: `(require '[malli.util :as mu])`

### Schema Manipulation

| Function | Purpose |
|----------|---------|
| `merge` | Merge map schemas |
| `union` | Union of schemas |
| `select-keys` | Select keys from map schema |
| `dissoc` | Remove keys from map schema |
| `assoc` | Add/replace key in map schema |
| `update` | Update key schema |
| `optional-keys` | Make all keys optional |
| `required-keys` | Make all keys required |
| `closed-schema` | Make map closed |
| `open-schema` | Make map open |

```clojure
;; Merge schemas
(mu/merge
  [:map [:a :int]]
  [:map [:b :string]])
;; => [:map [:a :int] [:b :string]]

;; Select keys
(mu/select-keys
  [:map [:a :int] [:b :string] [:c :boolean]]
  [:a :b])
;; => [:map [:a :int] [:b :string]]

;; Make all keys optional
(mu/optional-keys [:map [:a :int] [:b :string]])
;; => [:map [:a {:optional true} :int] [:b {:optional true} :string]]
```

## Performance Tips

1. **Cache validators/decoders for hot paths:**
```clojure
;; Bad - creates validator every call
(defn process [data]
  (when (m/validate schema data) ...))

;; Good - cached validator
(def valid? (m/validator schema))
(defn process [data]
  (when (valid? data) ...))
```

2. **Use coercer for input validation:**
```clojure
(def parse-request (m/coercer request-schema (mt/string-transformer)))
;; Throws with details on invalid input
```

3. **Decode doesn't validate:**
```clojure
;; decode returns invalid data as-is
(m/decode :int "bad" (mt/string-transformer)) ;; => "bad"

;; coerce throws on invalid
(m/coerce :int "bad" (mt/string-transformer)) ;; => throws
```
