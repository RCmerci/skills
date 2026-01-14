# Malli Registry Patterns

Registries allow reusable, named schemas. Choose the right approach based on your needs.

## Table of Contents
- [Using Clojure Vars (Simplest)](#using-clojure-vars-simplest)
- [Local Registries](#local-registries)
- [Global Registry](#global-registry)
- [Schema References](#schema-references)
- [Composite Registries](#composite-registries)

## Using Clojure Vars (Simplest)

Clojure vars work as schema references out of the box:

```clojure
(def UserId :uuid)

(def Address
  [:map
   [:street :string]
   [:city :string]
   [:zip :string]])

(def User
  [:map
   [:id UserId]           ;; references UserId var
   [:name :string]
   [:address Address]])   ;; references Address var

(m/validate User
  {:id #uuid "..."
   :name "Alice"
   :address {:street "123 Main" :city "Boston" :zip "02101"}})
```

**When to use:** Simple cases, single namespace, no need for lazy loading.

## Local Registries

Pass a registry in schema options for namespaced, self-contained schemas:

```clojure
(def my-registry
  {::user-id :uuid
   ::email [:string {:min 5}]
   ::address [:map
              [:street :string]
              [:city :string]]
   ::user [:map
           [:id ::user-id]
           [:email ::email]
           [:address ::address]]})

;; Use with :schema wrapper and :registry option
(def User [:schema {:registry my-registry} ::user])

(m/validate User
  {:id #uuid "..."
   :email "a@b.com"
   :address {:street "123 Main" :city "Boston"}})
```

**When to use:** Library code, isolated modules, avoiding global state.

## Global Registry

Set a default registry for the entire application:

```clojure
(require '[malli.registry :as mr])

;; Add to default registry
(mr/set-default-registry!
  (mr/composite-registry
    (m/default-schemas)  ;; keep builtins
    {::email [:re #".+@.+\..+"]
     ::user [:map
             [:name :string]
             [:email ::email]]}))

;; Now use anywhere without :registry option
(m/validate ::user {:name "Alice" :email "a@b.com"})
```

**When to use:** Application-wide schemas, shared across namespaces.

## Schema References

### :ref - Reference by Name

```clojure
(def registry
  {:user [:map
          [:name :string]
          [:friends [:vector [:ref :user]]]]})  ;; recursive!

(def User [:schema {:registry registry} :user])
```

### :schema - Inline Schema with Registry

```clojure
;; Wrap schema with local registry
[:schema {:registry {::pos-int [:int {:min 1}]}}
 [:map [:count ::pos-int]]]
```

## Composite Registries

Layer multiple registries (searched in order):

```clojure
(require '[malli.registry :as mr])

(def base-schemas
  {:email [:re #".+@.+"]
   :phone [:re #"\d{10}"]})

(def domain-schemas
  {:user [:map [:email :email] [:phone :phone]]})

;; Combine them
(mr/set-default-registry!
  (mr/composite-registry
    (m/default-schemas)    ;; malli builtins first
    base-schemas           ;; then base
    domain-schemas))       ;; then domain (can reference base)
```

## Registry Types

| Type | Purpose | Example |
|------|---------|---------|
| `simple-registry` | Basic map wrapper | `(mr/simple-registry {...})` |
| `fast-registry` | Optimized HashMap | `(mr/fast-registry {...})` |
| `composite-registry` | Layer multiple registries | `(mr/composite-registry r1 r2)` |
| `mutable-registry` | Wraps atom (hot reload) | `(mr/mutable-registry (atom {...}))` |
| `lazy-registry` | Load on demand | `(mr/lazy-registry default-reg provider-fn)` |

## Common Patterns

### Base + Domain Layering

```clojure
;; base-schemas.clj
(def base
  {:non-empty-string [:string {:min 1}]
   :email [:re #".+@.+\..+"]
   :uuid-str [:re #"^[0-9a-f-]{36}$"]})

;; user-schemas.clj
(def user-schemas
  {:user/id :uuid
   :user/email :email
   :user/user [:map
               [:id :user/id]
               [:email :user/email]]})

;; registry.clj
(mr/set-default-registry!
  (mr/composite-registry
    (m/default-schemas)
    base
    user-schemas))
```

### Recursive Schemas

```clojure
(def registry
  {:tree [:or
          :int
          [:map
           [:value :int]
           [:left [:ref :tree]]
           [:right [:ref :tree]]]]})

(def Tree [:schema {:registry registry} :tree])

(m/validate Tree
  {:value 1
   :left {:value 2 :left 3 :right 4}
   :right 5})
```

### Mutual Recursion

```clojure
(def registry
  {:ping [:map [:pong [:ref :pong]]]
   :pong [:map [:ping [:ref :ping]]]})
```
