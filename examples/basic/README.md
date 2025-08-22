# Basic Example

This example shows simple app configuration templating.

## Usage

```clojure
(require '[edn-templ.core :as templ])

;; Development config
(templ/eval-template "examples/basic/app-config.edn" "examples/basic/development.edn")

;; Production config  
(templ/eval-template "examples/basic/app-config.edn" "examples/basic/production.edn")

;; Output as JSON
(templ/eval-template "examples/basic/app-config.edn" "examples/basic/production.edn" :output :json)
```
