(ns bigco-pricecheck.utils)

(defn file-check
  "Returns a vector with:
  - boolean indicating whether in-path both exists and is not a directory
  - nil for true, or keyword explanation:  :not-found -or- :directory"
  [in-path]
  (let [file-obj (clojure.java.io/file in-path)
        exists? (.exists file-obj)
        directory? (.isDirectory file-obj)]
    (cond
      (and exists? (not directory?)) [true nil]
      directory? [false :directory]
      (not exists?) [false :not-found])))
