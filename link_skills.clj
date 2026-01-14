#!/usr/bin/env bb
(ns link-skills
  (:require [babashka.fs :as fs]))

(defn -main []
  (let [src-root (fs/absolutize ".")
        dst-root (fs/absolutize (fs/path (fs/home) ".codex" "skills"))]
    (when-not (fs/exists? src-root)
      (println "Source directory does not exist:" src-root)
      (System/exit 1))
    (fs/create-dirs dst-root)
    ;; Remove existing symlinks in destination root only.
    (doseq [p (fs/list-dir dst-root)
            :when (fs/sym-link? p)]
      (fs/delete-if-exists p))
    (doseq [p (fs/list-dir src-root)
            :when (and (fs/directory? p)
                       (not (.startsWith (str (fs/file-name p)) ".")))]
      (let [dst (fs/path dst-root (fs/file-name p))]
        (when (fs/exists? dst)
          (fs/delete-if-exists dst))
        (println "ln -s" (str p) "->" (str dst))
        (fs/create-sym-link dst p)))))

(-main)
