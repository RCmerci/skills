#!/usr/bin/env bb
(ns link-commands
  (:require
   [babashka.fs :as fs]
   [clojure.string :as str]))

(defn repo-root-from-script [script-file]
  (-> script-file
      fs/path
      fs/parent
      fs/absolutize))

(defn source-root-from-script [script-file]
  (fs/path (repo-root-from-script script-file) "commands"))

(defn destination-roots []
  [(fs/path (fs/home) ".config" "eca" "commands")
   (fs/path (fs/home) ".codex" "prompts")])

(defn discover-command-files [source-root]
  (let [source-root (fs/path source-root)]
    (if-not (fs/directory? source-root)
      []
      (->> (fs/list-dir source-root)
           (filter fs/regular-file?)
           (filter #(= "md" (-> % fs/extension str/lower-case)))
           (sort-by (comp str fs/file-name))
           vec))))

(defn ensure-link-target-slot! [destination-path]
  (cond
    (fs/sym-link? destination-path)
    (fs/delete-if-exists destination-path)

    (fs/exists? destination-path)
    (throw (ex-info (str "Refusing to overwrite non-symlink destination: " destination-path)
                    {:type :skills-repo.command-linking/non-symlink-conflict
                     :destination (str destination-path)}))

    :else nil))

(defn link-commands! [source-root destination-roots]
  (let [source-root (fs/path source-root)]
    (when-not (fs/directory? source-root)
      (throw (ex-info (str "Source commands directory does not exist: " source-root)
                      {:type :skills-repo.command-linking/missing-source-root
                       :source-root (str source-root)})))
    (let [command-files (discover-command-files source-root)]
      (reduce
       (fn [summary destination-root]
         (let [destination-root (fs/path destination-root)]
           (fs/create-dirs destination-root)
           (reduce
            (fn [inner-summary command-file]
              (let [link-path (fs/path destination-root (fs/file-name command-file))]
                (ensure-link-target-slot! link-path)
                (fs/create-sym-link link-path command-file)
                (update inner-summary :linked inc)))
            summary
            command-files)))
       {:linked 0}
       destination-roots))))

(defn -main []
  (try
    (let [source-root (source-root-from-script *file*)
          destination-roots (destination-roots)
          {:keys [linked]} (link-commands! source-root destination-roots)]
      (println "Linked" linked "command symlink(s).")
      0)
    (catch clojure.lang.ExceptionInfo e
      (binding [*out* *err*]
        (println (.getMessage e)))
      1)))

(let [exit-code (-main)]
  (when (not= 0 exit-code)
    (System/exit exit-code)))
