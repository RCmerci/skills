(ns clojuredocs-to-logseq.test-runner
  (:require
   [babashka.cli :as cli]
   [clojure.string :as str]
   [clojure.test :as t]
   [clojuredocs-to-logseq.import-validation-test]
   [clojuredocs-to-logseq.logseq-edn-test]
   [clojuredocs-to-logseq.transform-test]
   [skills-repo.skill-linking-test]))

(def test-namespaces
  '[clojuredocs-to-logseq.transform-test
    clojuredocs-to-logseq.logseq-edn-test
    clojuredocs-to-logseq.import-validation-test
    skills-repo.skill-linking-test])

(defn- fail! [message]
  (throw (ex-info message {:type :clojuredocs-to-logseq.test-runner/error})))

(defn- failing? [{:keys [fail error]}]
  (pos? (+ (long (or fail 0))
           (long (or error 0)))))

(defn- parse-focus [focus]
  (let [[ns-name test-name] (str/split focus #"/" 2)]
    (when (or (str/blank? ns-name)
              (str/blank? test-name))
      (fail! (str "Invalid --focus value: " focus)))
    [(symbol ns-name) (symbol test-name)]))

(defn- run-focused! [focus]
  (let [[ns-sym test-sym] (parse-focus focus)]
    (require ns-sym)
    (let [test-var (ns-resolve ns-sym test-sym)]
      (when-not test-var
        (fail! (str "Could not resolve test var: " focus)))
      (let [result (binding [t/*report-counters* (ref t/*initial-report-counters*)]
                     (t/test-vars [test-var])
                     @t/*report-counters*)]
        (when (failing? result)
          (throw (ex-info "Focused tests failed"
                          {:type :clojuredocs-to-logseq.test-runner/failing-tests
                           :focus focus
                           :result result})))))))

(defn- run-all! []
  (let [result (apply t/run-tests test-namespaces)]
    (when (failing? result)
      (throw (ex-info "Test suite failed"
                      {:type :clojuredocs-to-logseq.test-runner/failing-tests
                       :result result})))))

(defn run! [args]
  (let [{:keys [focus]} (cli/parse-opts args {:coerce {:focus :string}})]
    (if focus
      (run-focused! focus)
      (run-all!))))
