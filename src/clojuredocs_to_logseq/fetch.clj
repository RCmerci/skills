(ns clojuredocs-to-logseq.fetch
  (:require
   [babashka.http-client :as http]
   [cheshire.core :as json]
   [clojure.java.io :as io]))

(defn fetch-json [url]
  (-> (http/get url)
      :body
      (json/parse-string true)))

(defn read-json-file [path]
  (with-open [reader (io/reader path)]
    (json/parse-stream reader true)))
