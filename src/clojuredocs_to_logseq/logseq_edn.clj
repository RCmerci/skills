(ns clojuredocs-to-logseq.logseq-edn)

(defn- page-entry [page]
  (let [blocks (vec (:build/children page))
        page' (dissoc page :build/children)]
    {:page page'
     :blocks blocks}))

(defn build-graph-export [skeleton model]
  (-> skeleton
      (assoc :pages-and-blocks (mapv page-entry (:pages-and-blocks model))
             :classes (:classes model)
             :properties (:properties model))))
