# Malli Schema Syntax Reference

## Table of Contents
- [Schema Structure](#schema-structure)
- [Primitive Types](#primitive-types)
- [Collection Types](#collection-types)
- [Map Schemas](#map-schemas)
- [Logical Operators](#logical-operators)
- [Sequence Schemas](#sequence-schemas)
- [Multi/Dispatch Schemas](#multidispatch-schemas)
- [Schema Properties](#schema-properties)

## Schema Structure

All schemas follow this data-driven format:

```clojure
;; Minimal form - just the type
:int
string?

;; Type with children
[:vector :int]

;; Type with properties (optional map in second position)
[:string {:min 1 :max 100}]

;; Type with properties and children
[:vector {:min 1} :int]
```

## Primitive Types

### Type Keywords (prefer these for clarity)

| Type | Validates | Example |
|------|-----------|---------|
| `:any` | Any value | `[:any]` |
| `:nil` | Only nil | `[:nil]` |
| `:some` | Non-nil values | `[:some]` |
| `:string` | Strings | `[:string {:min 1 :max 100}]` |
| `:int` | Integers | `[:int {:min 0 :max 100}]` |
| `:double` | Doubles | `[:double]` |
| `:float` | Floats | `[:float]` |
| `:boolean` | true/false | `[:boolean]` |
| `:keyword` | Keywords | `[:keyword]` |
| `:qualified-keyword` | Namespaced keywords | `[:qualified-keyword]` |
| `:symbol` | Symbols | `[:symbol]` |
| `:qualified-symbol` | Namespaced symbols | `[:qualified-symbol]` |
| `:uuid` | UUIDs | `[:uuid]` |

### Predicate Functions (also valid as schema types)

```clojure
;; These work directly as schemas
int?              ;; integers
string?           ;; strings
keyword?          ;; keywords
pos-int?          ;; positive integers
neg-int?          ;; negative integers
nat-int?          ;; natural integers (>= 0)
pos?              ;; positive numbers
neg?              ;; negative numbers
zero?             ;; zero
number?           ;; any number
boolean?          ;; booleans
uuid?             ;; UUIDs
inst?             ;; instants/dates
uri?              ;; URIs
map?              ;; maps
vector?           ;; vectors
set?              ;; sets
sequential?       ;; sequential collections
coll?             ;; any collection
ifn?              ;; things that implement IFn
fn?               ;; functions
```

### Comparator Schemas

```clojure
[:> 5]            ;; greater than 5
[:>= 0]           ;; >= 0
[:< 100]          ;; < 100
[:<= 50]          ;; <= 50
[:= "exact"]      ;; exactly equals "exact"
[:not= nil]       ;; not equal to nil
```

### Regex Schemas

```clojure
[:re #"^[a-z]+$"]           ;; matches regex
[:re #".+@.+\..+"]          ;; simple email pattern
[:string {:pattern #"\d+"}] ;; string with pattern property
```

## Collection Types

| Type | Validates | Example |
|------|-----------|---------|
| `:vector` | Vectors of type | `[:vector :int]` |
| `:set` | Sets of type | `[:set :keyword]` |
| `:sequential` | Sequential collections | `[:sequential :int]` |
| `:seqable` | Any seqable | `[:seqable :any]` |
| `:every` | First N elements (bounded check) | `[:every :int]` |

```clojure
;; Vector of integers
[:vector :int]

;; Vector with size constraints
[:vector {:min 1 :max 10} :int]

;; Set of keywords
[:set :keyword]

;; Sequential (lazy-friendly)
[:sequential :int]
```

## Map Schemas

### Basic Map Schema

```clojure
[:map
 [:name :string]
 [:age :int]
 [:email :string]]
```

### Map Entry Syntax

```clojure
;; Required key (default)
[:key-name schema]

;; Optional key
[:key-name {:optional true} schema]

;; Key with properties
[:key-name {:optional true :default "value"} schema]
```

### Map Properties

```clojure
;; Open map (default) - allows extra keys
[:map
 [:id :int]]

;; Closed map - rejects extra keys
[:map {:closed true}
 [:id :int]]
```

### Map-of (uniform key/value types)

```clojure
;; Map from keywords to integers
[:map-of :keyword :int]

;; Map from strings to any value
[:map-of :string :any]

;; With constraints
[:map-of {:min 1 :max 100} :keyword :string]
```

### Nested Maps

```clojure
[:map
 [:user [:map
         [:name :string]
         [:address [:map
                    [:street :string]
                    [:city :string]
                    [:zip :string]]]]]
```

## Logical Operators

### :and (all must pass)

```clojure
;; Positive integer
[:and :int [:> 0]]

;; String between 1-100 chars matching pattern
[:and
 [:string {:min 1 :max 100}]
 [:re #"^[a-zA-Z]"]]
```

### :or (any must pass)

```clojure
;; String or integer
[:or :string :int]

;; String, nil, or keyword
[:or :string :nil :keyword]
```

### :orn (named alternatives - useful for parsing)

```clojure
[:orn
 [:by-id :int]
 [:by-name :string]
 [:by-uuid :uuid]]

;; When parsed, returns tagged value:
;; (m/parse schema 42) => [:by-id 42]
```

### :not (negation)

```clojure
;; Anything except nil
[:not :nil]

;; Anything except empty string
[:not [:= ""]]
```

### :maybe (nilable)

```clojure
;; Integer or nil
[:maybe :int]

;; Equivalent to:
[:or :int :nil]
```

### :enum (one of fixed values)

```clojure
[:enum "draft" "published" "archived"]
[:enum :admin :user :guest]
[:enum 1 2 3 4 5]
```

## Sequence Schemas

For parsing sequential data (like command args, DSLs):

### Basic Quantifiers

```clojure
;; Zero or more
[:* :int]               ;; matches [], [1], [1 2 3], etc.

;; One or more
[:+ :int]               ;; matches [1], [1 2], but not []

;; Zero or one
[:? :int]               ;; matches [], [1], but not [1 2]

;; Exact repetition range
[:repeat {:min 2 :max 4} :int]  ;; 2-4 integers
```

### :cat (concatenation)

```clojure
;; Sequence of keyword, then int, then string
[:cat :keyword :int :string]
;; matches [:foo 42 "bar"]
```

### :catn (named concatenation)

```clojure
[:catn
 [:op :keyword]
 [:count :int]
 [:label :string]]

;; When parsed: {:op :foo :count 42 :label "bar"}
```

### :alt (alternation)

```clojure
[:alt :int :string]     ;; int or string at this position
```

### :altn (named alternation)

```clojure
[:altn
 [:num :int]
 [:text :string]]

;; When parsed: [:num 42] or [:text "hello"]
```

### Combining Sequence Schemas

```clojure
;; Command-line style: cmd [--flag value]* args+
[:cat
 :keyword                        ;; command
 [:* [:cat [:= "--"] :keyword :string]]  ;; options
 [:+ :string]]                   ;; positional args
```

## Tuple Schemas

Fixed-length vectors with positional types:

```clojure
;; [x y] coordinate
[:tuple :double :double]

;; [id name active?]
[:tuple :int :string :boolean]

;; With properties
[:tuple {:decode/json vec} :int :string]
```

## Multi/Dispatch Schemas

For polymorphic data (discriminated unions):

```clojure
[:multi {:dispatch :type}
 [:user [:map
         [:type [:= :user]]
         [:name :string]
         [:email :string]]]
 [:admin [:map
          [:type [:= :admin]]
          [:name :string]
          [:permissions [:set :keyword]]]]]
```

### Dispatch Options

```clojure
;; Dispatch by keyword
[:multi {:dispatch :type} ...]

;; Dispatch by function
[:multi {:dispatch (fn [x] (if (string? x) :string :other))}
 [:string :string]
 [:other :any]]

;; Default case
[:multi {:dispatch :type}
 [:known-type schema]
 [::m/default :any]]        ;; catches unmatched
```

## Schema Properties

Properties are metadata passed in an optional map:

### Size Constraints

```clojure
[:string {:min 1 :max 100}]     ;; string length
[:int {:min 0 :max 1000}]       ;; numeric range
[:vector {:min 1 :max 10} :int] ;; collection size
```

### Map Entry Properties

```clojure
[:map
 [:required-key :string]                    ;; required (default)
 [:optional-key {:optional true} :string]   ;; optional
 [:with-default {:default "foo"} :string]]  ;; default value
```

### Generation Properties (for malli.generator)

```clojure
[:string {:gen/min 5 :gen/max 10}]          ;; override generation bounds
[:string {:gen/fmap str/upper-case}]        ;; transform generated value
[:string {:gen/elements ["a" "b" "c"]}]     ;; pick from these values
```

### Transformation Properties

```clojure
;; Custom decoder for this schema
[:vector {:decode/string #(str/split % #",")} :int]

;; Custom encoder
[:inst {:encode/string #(.format (SimpleDateFormat. "yyyy-MM-dd") %)}]
```

### Error Message Properties

```clojure
[:string {:min 3
          :error/message "Name must be at least 3 characters"}]

;; Localized messages
[:int {:error/message {:en "must be integer"
                       :fi "täytyy olla kokonaisluku"}}]
```

### JSON Schema Properties

```clojure
[:string {:json-schema/title "User Name"
          :json-schema/description "The user's display name"}]
```

## Function Schemas

For validating function signatures:

```clojure
;; Single arity: int -> int
[:=> [:cat :int] :int]

;; Multiple arities
[:function
 [:=> [:cat :int] :int]
 [:=> [:cat :int :int] :int]]

;; With varargs
[:=> [:cat :int [:* :int]] :int]
```
