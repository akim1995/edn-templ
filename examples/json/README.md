# JSON Example

This example shows JSON templating with tagged literals.

## Usage

```clojure
(require '[edn-templ.core :as templ])

;; JSON template with JSON context
(templ/eval-template "examples/json/api-config.json" 
                     "examples/json/variables.json" 
                     :tagged-json? true)

;; Custom tag prefix
;; (templ/eval-template "template.json" "vars.json" 
;;                      :tagged-json? true 
;;                      :tag-prefix "@@")
```
