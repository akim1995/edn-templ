# File Inclusion Example

This example shows how to split large configurations into smaller, manageable files.

## Usage

```clojure
(require '[edn-templ.core :as templ])

;; Load main config that includes other files
(templ/eval-template "examples/file-inclusion/main-config.edn" 
                     "examples/file-inclusion/context.edn")
```

## What it demonstrates

- `#file` - Include entire files as data structures
- `#splice-file` - **Splice** array contents inline
- `#raw-file` - Include raw text content as strings
- Modular configuration management

## Key difference: #file vs #splice-file

- `#file "features.edn"` → includes `["a", "b", "c"]` as a nested vector
- `#splice-file "features.edn"` → splices into `["before", "a", "b", "c", "after"]`
