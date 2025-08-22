(ns edn-templ.eval
  "Template variable resolution and interpolation.
  
  Provides functions for resolving context variables and interpolating
  template strings with ${variable} syntax. Handles both keyword and
  string keys for JSON/EDN compatibility."
  (:require [clojure.string :as str]))

;; Template evaluation logic

(defn get-nested
  "Gets value from context at path. Tries both keyword and string keys 
  for JSON compatibility. Path can be keyword or vector.
  Returns [value found?] tuple to distinguish missing keys from nil values."
  [context path]
  (if (keyword? path)
    ;; Try keyword first, then string fallback for JSON contexts
    (if (contains? context path)
      [(get context path) true]
      (let [str-key (name path)]
        (if (contains? context str-key)
          [(get context str-key) true]
          [nil false])))
    ;; For path vectors, try both keyword and string keys
    (let [kw-path path
          str-path (mapv #(if (keyword? %) (name %) %) path)]
      (if (not= ::not-found (get-in context kw-path ::not-found))
        [(get-in context kw-path) true]
        (if (not= ::not-found (get-in context str-path ::not-found))
          [(get-in context str-path) true]
          [nil false])))))

(defn resolve-variable
  "Resolves slash-separated variable expression like 'host' or 'config/db' 
  against context. Throws ex-info if variable is not found."
  [context var-expr]
  (let [var-path (mapv keyword (str/split var-expr #"/"))
        [value found?] (get-nested context var-path)]
    (when-not found?
      (throw (ex-info "Template variable not found"
                      {:variable var-expr
                       :path var-path
                       :context-keys (keys context)
                       :type :variable-not-found})))
    value))

(defn interpolate-template
  "Replaces ${var} expressions in string s with values from context."
  [s context]
  (try
    (str/replace s #"\$\{([^}]+)\}"
                 (fn [[_ var-name]]
                   (str (resolve-variable context var-name))))
    (catch Exception e
      (throw (ex-info "Template interpolation failed"
                      {:template s
                       :original-error (.getMessage e)
                       :type :interpolation-error}
                      e)))))