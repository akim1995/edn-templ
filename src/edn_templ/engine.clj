(ns edn-templ.engine
  "Core template processing engine.

  Provides file loading, path resolution, and template evaluation functionality
  for both EDN and JSON template files with support for tagged literals."
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [cheshire.core :as json]
            [clojure.walk :as walk]
            [edn-templ.readers :refer [template-readers]]
            [edn-templ.json :as templ-json]
            [edn-templ.eval :as templ-eval]))

;; Resource loading utilities

(defn resolve-path
  "Returns File or resource for path. Absolute paths (starting with /) 
  use filesystem only. Relative paths try filesystem first, then classpath.
  Throws ex-info if not found."
  [path]
  (if (.startsWith path "/")
    (let [file (io/file path)]
      (if (.exists file)
        file
        (throw (ex-info "Filesystem file not found"
                        {:path path :type :file-not-found}))))
    (let [file (io/file path)]
      (cond
        (.exists file) file
        :else          (or (io/resource path)
                           (throw (ex-info "File not found"
                                           {:path    path
                                            :type    :file-not-found
                                            :tried   [:filesystem :classpath]})))))))

(defn load-raw-file
  "Returns the content of path as a string."
  [path]
  (slurp (resolve-path path)))

(defn read-template-edn
  "Reads EDN string s with template readers applied."
  [s]
  (edn/read-string {:readers template-readers} s))

(declare evaluate-template)

(defn load-file-auto
  "Loads path, auto-detecting format. .edn files are parsed with template 
  readers and evaluated. .json files can optionally process tagged literals 
  with :tagged-json? true. Other files return as raw strings."
  ([path context templates]
   (load-file-auto path context templates {} #{}))
  ([path context templates & {:keys [tagged-json? tag-prefix visited]
                              :or   {tagged-json? false
                                     tag-prefix   "~#"
                                     visited      #{}}}]
   (when (contains? visited path)
     (throw (ex-info "Circular file inclusion detected"
                     {:path path
                      :inclusion-chain (vec visited)
                      :type :circular-inclusion})))
   (let [res (resolve-path path)
         new-visited (conj visited path)]
     (cond
       (.endsWith path ".edn")
       (let [loaded (edn/read-string {:readers template-readers} (slurp res))]
         (evaluate-template loaded context templates tagged-json? tag-prefix new-visited))

       (.endsWith path ".json")
       (let [loaded (json/parse-string (slurp res) true)]
         (if tagged-json?
           (let [parsed (templ-json/parse-json-tagged-literals loaded tag-prefix)]
             (evaluate-template parsed context templates tagged-json? tag-prefix new-visited))
           loaded))

       :else
       (slurp res)))))

(defn preprocess-context
  "Returns context with #file, #raw-file, and #splice-file references resolved."
  [context]
  (walk/postwalk
   (fn [node]
     (cond
       (and (map? node) (= (:edn-template/type node) :file))
       (load-file-auto (:path node) {} {} :visited #{})

       (and (map? node) (= (:edn-template/type node) :raw-file))
       (load-raw-file (:path node))

       (and (map? node) (= (:edn-template/type node) :splice-file))
       (vec (load-file-auto (:path node) {} {} :visited #{}))

       :else node))
   context))

(defn load-context
  "Loads and preprocesses context from resource-name (.edn or .json file)."
  [resource-name]
  (let [content (slurp (resolve-path resource-name))]
    (preprocess-context
     (if (.endsWith resource-name ".json")
       (json/parse-string content true)
       (read-template-edn content)))))

(defn load-template
  "Loads template from resource-name EDN file with template readers."
  [resource-name]
  (-> resource-name
      resolve-path
      slurp
      read-template-edn))

;; Template evaluation (needs to be defined here due to circular dependency with load-file-auto)

(defn evaluate-template
  "Evaluates template with context, recursively processing template types:
  :template (${var} interpolation), :ref (context references), :file 
  (includes), :raw-file (raw content), :splice-file (splicing)."
  ([template context templates]
   (evaluate-template template context templates false "~#" #{}))
  ([template context templates tagged-json? tag-prefix]
   (evaluate-template template context templates tagged-json? tag-prefix #{}))
  ([template context templates tagged-json? tag-prefix visited]
   (cond
     (and (map? template) (= (:edn-template/type template) :template))
     (templ-eval/interpolate-template (:template template) context)

     (and (map? template) (= (:edn-template/type template) :ref))
     (let [[value found?] (templ-eval/get-nested context (:path template))]
       (when-not found?
         (throw (ex-info "Reference not found"
                         {:ref-path (:path template)
                          :context-keys (keys context)
                          :type :ref-not-found})))
       value)

     (and (map? template) (= (:edn-template/type template) :file))
     (load-file-auto (:path template) context templates
                     :tagged-json? tagged-json?
                     :tag-prefix tag-prefix
                     :visited visited)

     (and (map? template) (= (:edn-template/type template) :raw-file))
     (load-raw-file (:path template))

     (and (map? template) (= (:edn-template/type template) :splice-file))
     (vec (load-file-auto (:path template) context templates
                          :tagged-json? tagged-json?
                          :tag-prefix tag-prefix
                          :visited visited))

     (map? template)
     (into {} (map (fn [[k v]]
                     [k (evaluate-template v context templates tagged-json? tag-prefix visited)])
                   template))

     (vector? template)
     (vec (mapcat
           (fn [el]
             (let [ev (evaluate-template el context templates tagged-json? tag-prefix visited)]
               (if (sequential? ev) ev [ev])))
           template))

     :else template)))

(defn render-template
  "Renders pre-loaded template with context. Convenience wrapper 
  around evaluate-template."
  ([template context]
   (render-template template context {}))
  ([template context templates & {:keys [tagged-json? tag-prefix]
                                  :or   {tagged-json? false
                                         tag-prefix   "~#"}}]
   (evaluate-template template context templates tagged-json? tag-prefix)))
