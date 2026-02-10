(ns clojuredocs-to-logseq.cli
  (:require
   [babashka.cli :as cli]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.string :as str]
   [clojuredocs-to-logseq.fetch :as fetch]
   [clojuredocs-to-logseq.logseq-edn :as logseq-edn]
   [clojuredocs-to-logseq.skeleton :as skeleton]
   [clojuredocs-to-logseq.transform :as transform]))

(def default-skeleton-path "test/fixtures/logseq/skeleton-export.edn")
(def default-timeout-ms 600000)
(def default-query
  "[:find (count ?p) . :where [?p :block/tags :user.class/ClojureDocsVar]]")

(defn run-command! [& args]
  (apply shell/sh args))

(defn- required-opt! [opts k]
  (or (get opts k)
      (throw (ex-info (str "Missing required option: " k)
                      {:type :clojuredocs-to-logseq.cli/missing-option
                       :option k}))))

(defn- parse-count [s]
  (let [trimmed (str/trim (or s ""))]
    (cond
      (str/blank? trimmed) 0
      :else (try
              (let [parsed (edn/read-string trimmed)]
                (cond
                  (number? parsed)
                  (long parsed)

                  (map? parsed)
                  (let [result (or (get-in parsed [:data :result])
                                   (:result parsed)
                                   0)]
                    (long result))

                  :else
                  (Long/parseLong trimmed)))
              (catch Exception _
                (Long/parseLong trimmed))))))

(defn convert-command [opts]
  (let [input-path (:input opts)
        input-url (:input-url opts)
        output-path (required-opt! opts :output)
        skeleton-path (or (:skeleton opts) default-skeleton-path)
        source (cond
                 input-url (fetch/fetch-json input-url)
                 input-path (fetch/read-json-file input-path)
                 :else (throw (ex-info "Either --input or --input-url is required"
                                       {:type :clojuredocs-to-logseq.cli/missing-input})))
        model (transform/clojuredocs-json->logseq-model source)
        skeleton-export (skeleton/read-skeleton-export skeleton-path)
        export (logseq-edn/build-graph-export skeleton-export model)]
    (io/make-parents output-path)
    (spit output-path (pr-str export))
    {:status :ok
     :output output-path
     :pages (count (:pages-and-blocks model))}))

(defn- ensure-zero-exit! [result context]
  (when-not (zero? (:exit result))
    (throw (ex-info (str context " failed")
                    {:type :clojuredocs-to-logseq.cli/command-failed
                     :context context
                     :result result})))
  result)

(defn validate-import-command [opts]
  (let [input (required-opt! opts :input)
        repo (required-opt! opts :repo)
        data-dir (:data-dir opts)
        timeout-ms (str (or (:timeout-ms opts) default-timeout-ms))
        logseq-prefix (cond-> ["logseq" "--timeout-ms" timeout-ms]
                        data-dir (into ["--data-dir" data-dir]))
        run-logseq! (fn [& args]
                      (apply run-command! (into logseq-prefix args)))
        _remove-result (run-logseq! "graph" "remove" "--repo" repo)
        import-result (run-logseq! "graph" "import"
                                   "--repo" repo
                                   "--type" "edn"
                                   "--input" input)
        _ (ensure-zero-exit! import-result "logseq graph import")
        query-result (run-logseq! "query"
                                  "--repo" repo
                                  "--output" "edn"
                                  "--query" default-query)
        _ (ensure-zero-exit! query-result "logseq query")
        var-page-count (parse-count (:out query-result))]
    {:status :ok
     :repo repo
     :data-dir data-dir
     :timeout-ms (Long/parseLong timeout-ms)
     :var-page-count var-page-count}))

(def dispatch-table
  [{:cmds ["convert"]
    :spec {:input {:coerce :string}
           :input-url {:coerce :string}
           :output {:coerce :string}
           :skeleton {:coerce :string}}
    :fn (fn [{:keys [opts]}] (convert-command opts))}
   {:cmds ["validate-import"]
    :spec {:input {:coerce :string}
           :repo {:coerce :string}
           :data-dir {:coerce :string}
           :timeout-ms {:coerce :long}}
    :fn (fn [{:keys [opts]}] (validate-import-command opts))}
   {:cmds []
    :fn (fn [_] (throw (ex-info "Unknown command" {})))}])

(defn -main [& args]
  (cli/dispatch dispatch-table args))
