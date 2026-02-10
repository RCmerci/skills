(ns skills-repo.skill-linking
  (:require
   [babashka.fs :as fs]
   [clojure.string :as str]))

(def skill-manifest-file "SKILL.md")
(def destination-roots-env "SKILLS_DEST_ROOTS")
(def source-root-env "SKILLS_SOURCE_ROOT")

(defn default-destination-roots []
  [(fs/path (fs/home) ".codex" "skills")
   (fs/path (fs/home) ".config" "opencode" "skills")])

(defn parse-destination-roots [value]
  (if (str/blank? value)
    (default-destination-roots)
    (->> (str/split value (re-pattern (java.util.regex.Pattern/quote java.io.File/pathSeparator)))
         (remove str/blank?)
         (map fs/path)
         vec)))

(defn destination-roots-from-env []
  (parse-destination-roots (System/getenv destination-roots-env)))

(defn repo-root-from-script [script-file]
  (-> script-file
      fs/path
      fs/parent
      fs/absolutize))

(defn source-root-from-script [script-file]
  (if-let [override (System/getenv source-root-env)]
    (fs/path override)
    (fs/path (repo-root-from-script script-file) "skills")))

(defn- canonical-path-str [path]
  (str (fs/canonicalize (fs/path path))))

(defn discover-skill-directories [source-root]
  (let [source-root (fs/path source-root)]
    (if-not (fs/directory? source-root)
      []
      (->> (fs/list-dir source-root)
           (filter fs/directory?)
           (filter #(fs/exists? (fs/path % skill-manifest-file)))
           (sort-by (comp str fs/file-name))
           vec))))

(defn- ensure-link-target-slot! [destination-path]
  (cond
    (fs/sym-link? destination-path)
    (fs/delete-if-exists destination-path)

    (fs/exists? destination-path)
    (throw (ex-info (str "Refusing to overwrite non-symlink destination: " destination-path)
                    {:type :skills-repo.skill-linking/non-symlink-conflict
                     :destination (str destination-path)}))

    :else nil))

(defn link-skills! [source-root destination-roots]
  (let [source-root (fs/path source-root)]
    (when-not (fs/directory? source-root)
      (throw (ex-info (str "Source skill directory does not exist: " source-root)
                      {:type :skills-repo.skill-linking/missing-source-root
                       :source-root (str source-root)})))
    (let [skills (discover-skill-directories source-root)]
      (reduce
       (fn [summary destination-root]
         (let [destination-root (fs/path destination-root)]
           (fs/create-dirs destination-root)
           (reduce
            (fn [inner-summary skill-directory]
              (let [link-path (fs/path destination-root (fs/file-name skill-directory))]
                (ensure-link-target-slot! link-path)
                (fs/create-sym-link link-path skill-directory)
                (update inner-summary :linked inc)))
            summary
            skills)))
       {:linked 0}
       destination-roots))))

(defn- owned-link? [owned-targets link-path]
  (try
    (contains? owned-targets (canonical-path-str (fs/real-path link-path)))
    (catch Throwable _
      false)))

(defn unlink-skills! [source-root destination-roots]
  (let [source-root (fs/path source-root)
        owned-targets (->> (discover-skill-directories source-root)
                           (map canonical-path-str)
                           set)]
    (reduce
     (fn [summary destination-root]
       (let [destination-root (fs/path destination-root)]
         (if-not (fs/directory? destination-root)
           summary
           (reduce
            (fn [inner-summary path]
              (if (and (fs/sym-link? path)
                       (owned-link? owned-targets path))
                (do
                  (fs/delete-if-exists path)
                  (update inner-summary :unlinked inc))
                inner-summary))
            summary
            (fs/list-dir destination-root)))))
     {:unlinked 0}
     destination-roots)))
