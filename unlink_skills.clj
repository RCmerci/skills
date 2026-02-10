#!/usr/bin/env bb
(ns unlink-skills
  (:require
   [skills-repo.skill-linking :as skill-linking]))

(defn -main []
  (try
    (let [source-root (skill-linking/source-root-from-script *file*)
          destination-roots (skill-linking/destination-roots-from-env)
          {:keys [unlinked]} (skill-linking/unlink-skills! source-root destination-roots)]
      (println "Unlinked" unlinked "skill symlink(s).")
      0)
    (catch clojure.lang.ExceptionInfo e
      (binding [*out* *err*]
        (println (.getMessage e)))
      1)))

(let [exit-code (-main)]
  (when (not= 0 exit-code)
    (System/exit exit-code)))
