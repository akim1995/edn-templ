(ns edn-templ.json
  "JSON tagged literal processing for template evaluation.
  
  Provides functionality to parse JSON objects with special tag prefixes
  (e.g., {\"~#interp\": \"${var}\"}) and convert them to internal template
  representation for later evaluation."
  (:require [clojure.string :as str]
            [clojure.walk :as walk]))

;; JSON tagged literal support

(defn parse-json-tagged-literals
  "Converts JSON objects with tag-prefix keys to template format. 
  Recursively processes ~#interp, ~#ref, ~#file, ~#splice-file, 
  and ~#raw-file tagged literals."
  ([obj] (parse-json-tagged-literals obj "~#"))
  ([obj tag-prefix]
   (walk/postwalk
    (fn [node]
      (if (and (map? node)
               (= 1 (count node))
               (let [k (first (keys node))]
                 (and (or (string? k) (keyword? k))
                      (str/starts-with? (if (keyword? k) (name k) k) tag-prefix))))
        ;; Convert tag to template reader format
        (let [[tag-key tag-value] (first node)
              tag-key-str (if (keyword? tag-key) (name tag-key) tag-key)
              tag-name (subs tag-key-str (count tag-prefix))]
          (case tag-name
            "interp"      {:edn-template/type :template
                           :template tag-value}
            "ref"         {:edn-template/type :ref
                           :path (if (vector? tag-value)
                                   (mapv keyword tag-value)
                                   (keyword tag-value))}
            "file"        {:edn-template/type :file
                           :path tag-value}
            "splice-file" {:edn-template/type :splice-file
                           :path tag-value}
            "raw-file"    {:edn-template/type :raw-file
                           :path tag-value}
            ;; Unknown tag, keep as-is
            node))
        node))
    obj)))