(ns edn-templ.core-test
  (:require [clojure.test :as test]
            [edn-templ.core :as templ]
            [cheshire.core :as cheshire]))

(test/deftest test-edn-templates
  (test/testing "Basic EDN template evaluation"
    (let [result (templ/eval-template "test/resources/template.edn" "test/resources/context.edn")]
      (test/is (map? result))
      (test/is (contains? result :app))
      (test/is (= "MyApp" (get-in result [:app :name])))
      (test/is (= "localhost:8080/api/v1" (get-in result [:app :url])))))

  (test/testing "Environment-specific contexts"
    (let [dev-result (templ/eval-template "test/resources/template.edn" "test/resources/context-dev.edn")
          prod-result (templ/eval-template "test/resources/template.edn" "test/resources/context-prod.edn")]
      (test/is (= "localhost:3000/api/v1" (get-in dev-result [:app :url])))
      (test/is (= "myapp.com/api/v1" (get-in prod-result [:app :url]))))))

(test/deftest test-json-templates
  (test/testing "JSON template with tagged literals"
    (let [result (templ/eval-template "test/resources/test-json-templates.json" "test/resources/context.json" :tagged-json? true)]
      (test/is (map? result))
      (test/is (= "MyApp" (get-in result [:app :name])))
      (test/is (= "localhost:8080/api/v1" (get-in result [:app :url])))
      (test/is (= 3000 (get-in result [:app :port])))
      (test/is (= "development" (get-in result [:app :environment])))))

  (test/testing "Custom tag prefix"
    ;; Create temp files for thtest/is test
    (let [temp-template (java.io.File/createTempFile "template" ".json")
          temp-context (java.io.File/createTempFile "context" ".json")]
      (spit temp-template "{\"url\": {\"@@interp\": \"${host}/api\"}}")
      (spit temp-context "{\"host\": \"example.com\"}")

      (let [result (templ/eval-template (.getAbsolutePath temp-template)
                                        (.getAbsolutePath temp-context)
                                        :tagged-json? true
                                        :tag-prefix "@@")]
        (test/is (= "example.com/api" (get result :url))))

      (.delete temp-template)
      (.delete temp-context))))

(test/deftest test-output-formats
  (test/testing "Default EDN output"
    (let [result (templ/eval-template "test/resources/template.edn" "test/resources/context.edn")]
      (test/is (map? result))))

  (test/testing "Pretty JSON output"
    (let [result (templ/eval-template "test/resources/template.edn" "test/resources/context.edn" :output :json)]
      (test/is (string? result))
      (test/is (.contains result "\n"))  ; Pretty formatted
      ;; Should be valid JSON
      (test/is (map? (cheshire/parse-string result true)))))

  (test/testing "Compact JSON output"
    (let [result (templ/eval-template "test/resources/template.edn" "test/resources/context.edn" :output :json-compact)]
      (test/is (string? result))
      (test/is (not (.contains result "\n")))  ; No formatting
      ;; Should be valid JSON
      (test/is (map? (cheshire/parse-string result true))))))

(test/deftest test-filesystem-paths
  (test/testing "Absolute filesystem paths"
    (let [temp-template (java.io.File/createTempFile "template" ".edn")
          temp-context (java.io.File/createTempFile "context" ".edn")
          template-path (.getAbsolutePath temp-template)
          context-path (.getAbsolutePath temp-context)]
      (try
        (spit temp-template "{:greeting #interp \"Hello ${name}!\"}")
        (spit temp-context "{:name \"World\"}")

        ;; Verify files extest/ist before calling eval-template
        (test/is (.exists temp-template) "Template file should extest/ist")
        (test/is (.exists temp-context) "Context file should extest/ist")

        (let [result (templ/eval-template template-path context-path)]
          (test/is (= {:greeting "Hello World!"} result)))

        (finally
          (.delete temp-template)
          (.delete temp-context)))))

 (test/deftest test-error-cases
   (test/testing "File not found errors"
     (test/is (thrown? Exception (templ/eval-template "mtest/issing.edn" "test/resources/context.edn")))
     (test/is (thrown? Exception (templ/eval-template "test/resources/template.edn" "mtest/issing.edn")))
     (test/is (thrown? Exception (templ/eval-template "/nonextest/istent/path.edn" "test/resources/context.edn"))))

   (test/testing "Missing variable errors"
     (let [temp-template (java.io.File/createTempFile "template" ".edn")
           temp-context (java.io.File/createTempFile "context" ".edn")]
       (try
         (spit temp-template "{:message #interp \"Hello ${missing-var}!\"}")
         (spit temp-context "{:name \"World\"}")

         (test/is (thrown-with-msg?
                   clojure.lang.ExceptionInfo #"Template interpolation failed"
                   (templ/eval-template (.getAbsolutePath temp-template)
                                        (.getAbsolutePath temp-context))))
         (finally
           (.delete temp-template)
           (.delete temp-context)))))

   (test/testing "Missing reference errors"
     (let [temp-template (java.io.File/createTempFile "template" ".edn")
           temp-context (java.io.File/createTempFile "context" ".edn")]
       (try
         (spit temp-template "{:config #ref :missing-key}")
         (spit temp-context "{:name \"World\"}")

         (test/is (thrown-with-msg?
                   clojure.lang.ExceptionInfo #"Reference not found"
                   (templ/eval-template (.getAbsolutePath temp-template)
                                        (.getAbsolutePath temp-context))))
         (finally
           (.delete temp-template)
           (.delete temp-context)))))

   (test/testing "Circular inclusion errors"
     (let [temp-a (java.io.File/createTempFile "aaa" ".edn")
           temp-b (java.io.File/createTempFile "bbb" ".edn")
           temp-context (java.io.File/createTempFile "context" ".edn")]
       (try
         (spit temp-a (str "{:data #file \"" (.getAbsolutePath temp-b) "\"}"))
         (spit temp-b (str "{:data #file \"" (.getAbsolutePath temp-a) "\"}"))
         (spit temp-context "{}")

         (test/is (thrown-with-msg?
                   clojure.lang.ExceptionInfo #"Circular file inclusion detected"
                   (templ/eval-template (.getAbsolutePath temp-a)
                                        (.getAbsolutePath temp-context))))
         (finally
           (.delete temp-a)
           (.delete temp-b)
           (.delete temp-context)))))))
