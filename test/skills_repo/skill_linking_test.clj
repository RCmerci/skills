(ns skills-repo.skill-linking-test
  (:require
   [babashka.fs :as fs]
   [clojure.test :refer [deftest is testing]]))

(defn- resolve-fn [sym]
  (try
    (requiring-resolve sym)
    (catch Throwable _
      nil)))

(defn- with-temp-dir! [f]
  (let [tmp (fs/create-temp-dir {:prefix "skills-repo-"})]
    (try
      (f tmp)
      (finally
        (fs/delete-tree tmp)))))

(defn- write-file! [path content]
  (fs/create-dirs (fs/parent path))
  (spit (str path) content))

(defn- create-skill-dir! [root skill-name]
  (let [skill-dir (fs/path root skill-name)]
    (fs/create-dirs skill-dir)
    (write-file! (fs/path skill-dir "SKILL.md") (str "# " skill-name))
    skill-dir))

(defn- create-dir-without-skill-file! [root dir-name]
  (let [dir (fs/path root dir-name)]
    (fs/create-dirs dir)
    dir))

(defn- path-basename-set [paths]
  (->> paths
       (map #(str (fs/file-name %)))
       set))

(defn- real-path-str [path]
  (str (fs/real-path path)))

(deftest discovers-only-skill-directories
  (with-temp-dir!
    (fn [tmp]
      (let [src-root (fs/path tmp "source")
            discover! (resolve-fn 'skills-repo.skill-linking/discover-skill-directories)]
        (create-skill-dir! src-root "planning-documents")
        (create-skill-dir! src-root "writing-plans")
        (create-dir-without-skill-file! src-root "docs")
        (create-dir-without-skill-file! src-root "src")

        (is (some? discover!) "discover-skill-directories must be defined")
        (when discover!
          (is (= #{"planning-documents" "writing-plans"}
                 (path-basename-set (discover! src-root)))))))))

(deftest creates-symlinks-for-each-skill
  (with-temp-dir!
    (fn [tmp]
      (let [src-root (fs/path tmp "source")
            destination-a (fs/path tmp "destination-a")
            destination-b (fs/path tmp "destination-b")
            link! (resolve-fn 'skills-repo.skill-linking/link-skills!)]
        (create-skill-dir! src-root "planning-documents")
        (create-skill-dir! src-root "writing-plans")
        (create-dir-without-skill-file! src-root "docs")

        (is (some? link!) "link-skills! must be defined")
        (when link!
          (link! src-root [destination-a destination-b])
          (doseq [destination [destination-a destination-b]
                  skill-name ["planning-documents" "writing-plans"]]
            (let [link-path (fs/path destination skill-name)
                  expected-target (fs/path src-root skill-name)]
              (testing (str "creates symlink for " skill-name " in " destination)
                (is (fs/exists? link-path))
                (is (fs/sym-link? link-path))
                (is (= (real-path-str expected-target)
                       (real-path-str link-path)))))))))))

(deftest fails-on-non-symlink-conflict
  (with-temp-dir!
    (fn [tmp]
      (let [src-root (fs/path tmp "source")
            destination (fs/path tmp "destination")
            conflicting-path (fs/path destination "planning-documents")
            link! (resolve-fn 'skills-repo.skill-linking/link-skills!)]
        (create-skill-dir! src-root "planning-documents")
        (fs/create-dirs destination)
        (fs/create-dirs conflicting-path)

        (is (some? link!) "link-skills! must be defined")
        (when link!
          (is (thrown-with-msg? clojure.lang.ExceptionInfo
                                #"non-symlink"
                                (link! src-root [destination])))
          (is (fs/exists? conflicting-path))
          (is (not (fs/sym-link? conflicting-path))))))))

(deftest unlinks-only-owned-symlinks
  (with-temp-dir!
    (fn [tmp]
      (let [src-root (fs/path tmp "source")
            destination (fs/path tmp "destination")
            unlink! (resolve-fn 'skills-repo.skill-linking/unlink-skills!)]
        (doseq [skill-name ["planning-documents" "writing-plans"]]
          (create-skill-dir! src-root skill-name))
        (fs/create-dirs destination)
        (doseq [skill-name ["planning-documents" "writing-plans"]]
          (let [link-path (fs/path destination skill-name)
                target (fs/path src-root skill-name)]
            (fs/create-sym-link link-path target)))

        (is (some? unlink!) "unlink-skills! must be defined")
        (when unlink!
          (unlink! src-root [destination])
          (doseq [skill-name ["planning-documents" "writing-plans"]]
            (is (not (fs/exists? (fs/path destination skill-name))))))))))

(deftest preserves-unrelated-symlinks
  (with-temp-dir!
    (fn [tmp]
      (let [src-root (fs/path tmp "source")
            destination (fs/path tmp "destination")
            unrelated-root (fs/path tmp "unrelated")
            unrelated-link (fs/path destination "unrelated-link")
            unlink! (resolve-fn 'skills-repo.skill-linking/unlink-skills!)]
        (create-skill-dir! src-root "planning-documents")
        (fs/create-dirs destination)
        (fs/create-dirs unrelated-root)
        (write-file! (fs/path unrelated-root "placeholder.txt") "keep me")
        (fs/create-sym-link unrelated-link unrelated-root)

        (is (some? unlink!) "unlink-skills! must be defined")
        (when unlink!
          (unlink! src-root [destination])
          (is (fs/exists? unrelated-link))
          (is (fs/sym-link? unrelated-link))
          (is (= (real-path-str unrelated-root)
                 (real-path-str unrelated-link))))))))
