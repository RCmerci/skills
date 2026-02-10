(ns clojuredocs-to-logseq.import-validation-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [clojuredocs-to-logseq.cli :as cli]))

(deftest validate-import-runs-logseq-commands
  (let [calls (atom [])
        opts {:input "/tmp/clojure-docs.edn"
              :repo "clojure-docs-20260210-v1"
              :data-dir "/tmp/logseq-cli-plan"}
        fake-sh (fn [& args]
                  (swap! calls conj args)
                  (cond
                    (str/includes? (str args) "graph import") {:exit 0 :out "Imported edn from /tmp/clojure-docs.edn" :err ""}
                    (str/includes? (str args) "query") {:exit 0 :out "{:status :ok, :data {:result 1}}" :err ""}
                    :else {:exit 0 :out "" :err ""}))]
    (with-redefs [clojure.java.shell/sh fake-sh]
      (let [result (cli/validate-import-command opts)]
        (testing "returns success summary"
          (is (= :ok (:status result)))
          (is (pos? (:var-page-count result))))
        (testing "invokes remove, import, and query commands"
          (is (>= (count @calls) 3))
          (is (some #(and (some #{"graph"} %)
                          (some #{"remove"} %))
                    @calls))
          (is (some #(and (some #{"graph"} %)
                          (some #{"import"} %))
                    @calls))
          (is (some #(some #{"query"} %) @calls)))))))

(deftest validate-import-omits-data-dir-when-not-provided
  (let [calls (atom [])
        opts {:input "/tmp/clojure-docs.edn"
              :repo "clojure-docs-20260210-v1"}
        fake-sh (fn [& args]
                  (swap! calls conj args)
                  (cond
                    (str/includes? (str args) "graph import") {:exit 0 :out "Imported edn from /tmp/clojure-docs.edn" :err ""}
                    (str/includes? (str args) "query") {:exit 0 :out "{:status :ok, :data {:result 1}}" :err ""}
                    :else {:exit 0 :out "" :err ""}))]
    (with-redefs [clojure.java.shell/sh fake-sh]
      (let [result (cli/validate-import-command opts)
            all-args (mapcat identity @calls)]
        (is (= :ok (:status result)))
        (is (not-any? #{"--data-dir"} all-args))))))
