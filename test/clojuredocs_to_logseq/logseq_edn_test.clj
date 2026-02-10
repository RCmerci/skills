(ns clojuredocs-to-logseq.logseq-edn-test
  (:require
   [clojure.edn :as edn]
   [clojure.test :refer [deftest is testing]]
   [clojuredocs-to-logseq.logseq-edn :as logseq-edn]))

(def skeleton-path "test/fixtures/logseq/skeleton-export.edn")

(defn read-skeleton []
  (edn/read-string (slurp skeleton-path)))

(deftest merges-model-into-skeleton-envelope
  (let [skeleton (read-skeleton)
        model {:pages-and-blocks [{:block/title "clojure.core/map"}]
               :classes {:ClojureDocs.Var {:description "var class"}}
               :properties {:ns {:type :string}}}
        export (logseq-edn/build-graph-export skeleton model)]
    (testing "required top-level keys are present"
      (is (contains? export :pages-and-blocks))
      (is (contains? export :classes))
      (is (contains? export :properties))
      (is (contains? export :logseq.db.sqlite.export/schema-version))
      (is (contains? export :logseq.db.sqlite.export/graph-files))
      (is (contains? export :logseq.db.sqlite.export/kv-values))
      (is (contains? export :logseq.db.sqlite.export/export-type)))
    (testing "model payload replaces mutable sections"
      (is (= (count (:pages-and-blocks model))
             (count (:pages-and-blocks export))))
      (is (= "clojure.core/map"
             (get-in export [:pages-and-blocks 0 :page :block/title])))
      (is (= (:classes model) (:classes export)))
      (is (= (:properties model) (:properties export))))
    (testing "skeleton envelope metadata is preserved"
      (is (= (:logseq.db.sqlite.export/schema-version skeleton)
             (:logseq.db.sqlite.export/schema-version export)))
      (is (= (:logseq.db.sqlite.export/export-type skeleton)
             (:logseq.db.sqlite.export/export-type export))))))

(deftest converts-pages-to-import-shape
  (let [skeleton (read-skeleton)
        model {:pages-and-blocks [{:block/title "clojure.core/map"
                                   :build/tags [:ClojureDocs.Var]
                                   :build/properties {:ns "clojure.core"}
                                   :build/children [{:block/title "doc"}]}]
               :classes {:ClojureDocs.Var {}}
               :properties {:ns {:logseq.property/type :default
                                 :block/title "ns"}}}
        export (logseq-edn/build-graph-export skeleton model)
        first-entry (first (:pages-and-blocks export))]
    (is (contains? first-entry :page))
    (is (contains? first-entry :blocks))
    (is (= "clojure.core/map" (get-in first-entry [:page :block/title])))
    (is (= [{:block/title "doc"}] (:blocks first-entry)))
    (is (not (contains? (get first-entry :page) :build/children)))))
