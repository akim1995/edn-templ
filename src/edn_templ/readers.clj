(ns edn-templ.readers
  "EDN tagged literal readers for template processing.
  
  Provides reader functions that convert EDN tagged literals into
  internal template representation maps for later evaluation.")

;; Template readers for EDN tagged literals

(defn template-reader
  "Returns template map for s containing ${variable} placeholders."
  [s]
  {:edn-template/type :template
   :template s})

(defn ref-reader
  "Returns reference map for accessing context at path."
  [path]
  {:edn-template/type :ref
   :path path})

(defn file-reader
  "Returns file inclusion map for spec (path string or options map)."
  [spec]
  (if (string? spec)
    {:edn-template/type :file
     :path spec}
    (merge {:edn-template/type :file} spec)))

(defn raw-file-reader
  "Returns raw file inclusion map for path."
  [path]
  {:edn-template/type :raw-file
   :path path})

(defn splice-file-reader
  "Returns splice file inclusion map for spec (path string or options map)."
  [spec]
  (if (string? spec)
    {:edn-template/type :splice-file
     :path spec}
    (merge {:edn-template/type :splice-file} spec)))

(def template-readers
  "Map of tag symbols to reader functions for EDN template processing."
  {'interp       template-reader
   'ref          ref-reader
   'file         file-reader
   'splice-file  splice-file-reader
   'raw-file     raw-file-reader})