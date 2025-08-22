(ns edn-templ.core
  (:require [edn-templ.engine :as engine]
            [cheshire.core :as json]))


(defn eval-template
  "Evaluates template-res with context-res, returning the rendered result.
  template-res and context-res can be file paths or maps. For JSON files,
  use :tagged-json? true to process ~# tagged literals. Output can be
  :json, :json-compact or EDN (default)."
  ([template-res]
   (eval-template template-res {}))
  ([template-res context-res & {:keys [tagged-json? tag-prefix output]
                                 :or   {tagged-json? false
                                        tag-prefix   "~#"}}]
   (let [context (if (map? context-res)
                   context-res
                   (engine/load-context context-res))
         result  (if (.endsWith template-res ".json")
                   (engine/load-file-auto template-res context {}
                                          :tagged-json? tagged-json?
                                          :tag-prefix   tag-prefix
                                          :visited #{})
                   (-> template-res
                       engine/load-template
                       (engine/evaluate-template context {}
                                                 tagged-json? tag-prefix #{})))]
     (case output
       :json         (json/generate-string result {:pretty true})
       :json-compact (json/generate-string result)
       result))))
