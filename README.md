# edn-templ

[![CI](https://github.com/akim1995/edn-templ/workflows/CI/badge.svg)](https://github.com/akim1995/edn-templ/actions)

A simple Clojure library for templating EDN and JSON files with variable substitution and file inclusion.

## Why This Exists

- **Structure stays as data**: composition (`#ref`, `#file`, `#splice-file`, `#raw-file`) uses real EDN reader tags — not strings parsed at render time. String values still use `${var}` interpolation inside `#interp`.
- **File composition**: split large config files into smaller pieces.
- **Minimal by design**: no loops, conditionals, or complex logic — just variables and file inclusion.
- **JSON, with caveats**: the same tags work in JSON via a sentinel-key convention (`{"~#interp": "${host}/api"}`) since JSON has no tagged literals. Less idiomatic than EDN, but lets you template JSON consumed by non-Clojure tools.

## Installation

Add to your `deps.edn`:

```clojure
{:deps {io.github.akim1995/edn-templ {:git/tag "v1.0.0"}}}
```

## Quick Start

```clojure
(require '[edn-templ.core :as templ])

;; Basic template evaluation
(templ/eval-template "template.edn" "context.edn")

;; JSON support
(templ/eval-template "template.json" "context.json" :tagged-json? true)

;; Output as JSON
(templ/eval-template "template.edn" "context.edn" :output :json)
```

## Example

**app-config.edn** (template):
```clojure
{:app {:name "MyApp"
       :url #interp "${host}/api"
       :database #ref :db-config
       :features #file "features.edn"
       :all-users ["admin" 
                   #splice-file "regular-users.edn"
                   "guest"]}}
```

**context.edn**:
```clojure
{:host "localhost:8080"
 :db-config {:host "postgres" :port 5432}}
```

**features.edn**:
```clojure
["auth" "logging" "metrics"]
```

**regular-users.edn**:
```clojure
["alice" "bob" "charlie"]
```

**Result**:
```clojure
(eval-template "app-config.edn" "context.edn")
;; =>
{:app {:name "MyApp"
       :url "localhost:8080/api"
       :database {:host "postgres" :port 5432}
       :features ["auth" "logging" "metrics"]
       :all-users ["admin" "alice" "bob" "charlie" "guest"]}}
```

## Available Tags

| Tag | Description | Example |
|-----|-------------|---------|
| `#interp` | Variable substitution | `#interp "${host}/api"` |
| `#ref` | Reference context value | `#ref :database` |
| `#file` | Include another template/file | `#file "config.edn"` |
| `#splice-file` | Splice file contents inline | `["item1" #splice-file "more-items.edn" "item2"]` |
| `#raw-file` | Include raw file content | `#raw-file "schema.sql"` |

In JSON, the same tags are written as single-key maps: `{"~#tag": "value"}`. Pass `:tagged-json? true` to activate the convention.

## Options

```clojure
;; JSON support
(eval-template "config.json" "vars.json" :tagged-json? true)

;; Output as JSON
(eval-template "config.edn" "vars.edn" :output :json)

;; Filesystem paths or classpath resources both work
(eval-template "/path/to/config.edn" "/path/to/vars.edn")
```
