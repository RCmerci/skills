(ns clojuredocs-to-logseq.transform
  (:require
   [clojure.set :as set]
   [clojure.string :as str]))

(def section-order
  ["doc" "arglists" "examples" "notes" "see-alsos"])

(def declared-properties
  {:source-id {:block/title "source-id"
               :logseq.property/type :default}
   :ns {:block/title "ns"
        :logseq.property/type :default}
   :var-name {:block/title "var-name"
              :logseq.property/type :default}
   :type {:block/title "type"
          :logseq.property/type :default}
   :href {:block/title "href"
          :logseq.property/type :default}
   :library-url {:block/title "library-url"
                 :logseq.property/type :default}
   :item-type {:block/title "item-type"
               :logseq.property/type :default}
   :arg-index {:block/title "arg-index"
               :logseq.property/type :number}
   :created-at {:block/title "created-at"
                :logseq.property/type :number}
   :updated-at {:block/title "updated-at"
                :logseq.property/type :number}
   :author-login {:block/title "author-login"
                  :logseq.property/type :default}
   :target-ns {:block/title "target-ns"
               :logseq.property/type :default}
   :target-name {:block/title "target-name"
                 :logseq.property/type :default}})

(defn- as-children [xs]
  (vec (remove nil? xs)))

(defn- compact-properties [m]
  (into {} (remove (comp nil? val) m)))

(defn- ns-tag [ns-name]
  (keyword (str "ClojureDocs.Ns." ns-name)))

(defn- section-block [title children]
  {:block/title title
   :build/children (vec children)})

(defn- doc-section [doc-text]
  (section-block
   "doc"
   (if (str/blank? (str doc-text))
     []
     [{:block/title (str doc-text)
       :build/tags [:ClojureDocs.Doc]
       :build/properties (compact-properties {:item-type "doc"})}])))

(defn- arglists-section [arglists]
  (section-block
   "arglists"
   (map-indexed
    (fn [idx arglist]
      {:block/title (str arglist)
       :build/tags [:ClojureDocs.Arglists]
       :build/properties (compact-properties {:item-type "arglist"
                                              :arg-index idx})})
    (or arglists []))))

(defn- example->block [example]
  {:block/title (str "example: " (:id example) "\n" (:body example))
   :build/tags [:ClojureDocs.Example]
   :build/properties (compact-properties {:item-type "example"
                                          :source-id (str (:id example))
                                          :created-at (:created-at example)
                                          :updated-at (:updated-at example)
                                          :author-login (get-in example [:author :login])})})

(defn- note->block [note]
  {:block/title (str "note: " (:id note) "\n" (:body note))
   :build/tags [:ClojureDocs.Note]
   :build/properties (compact-properties {:item-type "note"
                                          :source-id (str (:id note))
                                          :created-at (:created-at note)
                                          :updated-at (:updated-at note)
                                          :author-login (get-in note [:author :login])})})

(defn- see-also->block [edge]
  (let [target-ns (get-in edge [:to-var :ns])
        target-name (get-in edge [:to-var :name])]
    {:block/title (str "see-also: " target-ns "/" target-name)
     :build/tags [:ClojureDocs.SeeAlso]
     :build/properties (compact-properties {:item-type "see-also"
                                            :source-id (str (:id edge))
                                            :target-ns target-ns
                                            :target-name target-name})}))

(defn- content-sections [{:keys [doc arglists examples notes see-alsos]}]
  [(doc-section doc)
   (arglists-section arglists)
   (section-block "examples" (map example->block (or examples [])))
   (section-block "notes" (map note->block (or notes [])))
   (section-block "see-alsos" (map see-also->block (or see-alsos [])))])

(defn- var->page [{:keys [id ns name type href library-url] :as source-var}]
  {:block/title (str ns "/" name)
   :build/tags [:ClojureDocs.Var (ns-tag ns)]
   :build/properties (compact-properties {:source-id (str id)
                                          :ns ns
                                          :var-name name
                                          :type type
                                          :href href
                                          :library-url library-url})
   :build/children (as-children (content-sections source-var))})

(defn clojuredocs-json->logseq-model [{:keys [vars]}]
  (let [pages (mapv var->page (or vars []))
        collect-tags
        (fn collect-tags [node]
          (set/union (set (:build/tags node))
                     (apply set/union #{} (map collect-tags (:build/children node)))))
        used-tags (apply set/union #{} (map collect-tags pages))
        classes (into {} (map (fn [class-tag] [class-tag {}])) used-tags)]
    {:pages-and-blocks pages
     :classes classes
     :properties declared-properties}))
