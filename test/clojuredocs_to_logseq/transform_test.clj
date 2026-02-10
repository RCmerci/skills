(ns clojuredocs-to-logseq.transform-test
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [clojuredocs-to-logseq.transform :as transform]))

(def fixture-path "test/fixtures/clojuredocs/sample.json")

(defn read-sample-json []
  (with-open [reader (io/reader fixture-path)]
    (json/parse-stream reader true)))

(defn page-by-title [model title]
  (some #(when (= title (:block/title %)) %) (:pages-and-blocks model)))

(defn child-by-title [parent title]
  (some #(when (= title (:block/title %)) %) (:build/children parent)))

(defn collect-build-property-keys [node]
  (let [self-keys (set (keys (:build/properties node)))
        child-keys (mapcat collect-build-property-keys (:build/children node))]
    (set/union self-keys (set child-keys))))

(defn collect-build-tags [node]
  (let [self-tags (set (:build/tags node))
        child-tags (mapcat collect-build-tags (:build/children node))]
    (set/union self-tags (set child-tags))))

(defn collect-nodes [node]
  (cons node (mapcat collect-nodes (:build/children node))))

(defn collect-property-values [node]
  (let [self-values (vals (:build/properties node))
        child-values (mapcat collect-property-values (:build/children node))]
    (concat self-values child-values)))

(deftest var-page-mapping
  (let [model (transform/clojuredocs-json->logseq-model (read-sample-json))
        page (page-by-title model "clojure.core/map")]
    (testing "var page exists with query-first identifiers"
      (is (some? page))
      (is (= "clojure.core" (get-in page [:build/properties :ns])))
      (is (= "map" (get-in page [:build/properties :var-name]))))
    (testing "page includes expected tags and section blocks"
      (is (contains? (set (:build/tags page)) :ClojureDocs.Var))
      (is (contains? (set (:build/tags page)) :ClojureDocs.Ns.clojure.core))
      (is (some? (child-by-title page "doc")))
      (is (some? (child-by-title page "arglists")))
      (is (some? (child-by-title page "examples")))
      (is (some? (child-by-title page "notes")))
      (is (some? (child-by-title page "see-alsos"))))))

(deftest examples-use-build-children
  (let [model (transform/clojuredocs-json->logseq-model (read-sample-json))
        page (page-by-title model "clojure.core/map")
        section (child-by-title page "examples")
        example (first (:build/children section))]
    (is (some? section))
    (is (contains? section :build/children))
    (is (not (contains? section :block/children)))
    (is (= 1 (count (:build/children section))))
    (is (some? example))
    (is (and (some? example)
             (str/starts-with? (:block/title example) "example: 201")))))

(deftest declares-user-properties-before-use
  (let [model (transform/clojuredocs-json->logseq-model (read-sample-json))
        page (page-by-title model "clojure.core/map")
        declared (set (keys (:properties model)))
        used (collect-build-property-keys page)
        expected #{:source-id :ns :var-name :type :href :library-url :item-type
                   :arg-index :created-at :updated-at :author-login
                   :target-ns :target-name}]
    (is (set/subset? expected declared))
    (is (set/subset? used declared))))

(deftest declares-all-used-classes
  (let [model (transform/clojuredocs-json->logseq-model (read-sample-json))
        page (page-by-title model "clojure.core/map")
        used-tags (collect-build-tags page)
        declared-classes (set (keys (:classes model)))]
    (is (set/subset? used-tags declared-classes))))

(deftest numeric-timestamp-properties-declared-as-number
  (let [model (transform/clojuredocs-json->logseq-model (read-sample-json))]
    (is (= :number (get-in model [:properties :created-at :logseq.property/type])))
    (is (= :number (get-in model [:properties :updated-at :logseq.property/type])))))

(deftest handles-null-notes-and-see-alsos
  (let [input (-> (read-sample-json)
                  (assoc-in [:vars 0 :notes] nil)
                  (assoc-in [:vars 0 :see-alsos] nil)
                  (assoc-in [:vars 0 :examples] nil))
        model (transform/clojuredocs-json->logseq-model input)
        page (page-by-title model "clojure.core/map")
        notes-section (child-by-title page "notes")
        see-also-section (child-by-title page "see-alsos")
        example-section (child-by-title page "examples")
        all-nodes (collect-nodes page)]
    (is (some? notes-section))
    (is (some? see-also-section))
    (is (some? example-section))
    (is (= [] (:build/children notes-section)))
    (is (= [] (:build/children see-also-section)))
    (is (= [] (:build/children example-section)))
    (is (not-any? nil? all-nodes))))

(deftest omits-nil-build-property-values
  (let [input (-> (read-sample-json)
                  (assoc-in [:vars 0 :href] nil)
                  (assoc-in [:vars 0 :library-url] nil)
                  (assoc-in [:vars 0 :examples 0 :created-at] nil)
                  (assoc-in [:vars 0 :examples 0 :updated-at] nil)
                  (assoc-in [:vars 0 :examples 0 :author :login] nil))
        model (transform/clojuredocs-json->logseq-model input)
        page (page-by-title model "clojure.core/map")
        values (collect-property-values page)]
    (is (not-any? nil? values))))
