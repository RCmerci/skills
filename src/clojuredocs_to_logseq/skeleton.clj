(ns clojuredocs-to-logseq.skeleton
  (:require
   [clojure.edn :as edn]))

(defn read-skeleton-export [path]
  (edn/read-string (slurp path)))
