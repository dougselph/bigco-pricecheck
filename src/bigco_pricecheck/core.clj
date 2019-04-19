(ns bigco-pricecheck.core
  (:require [bigco-pricecheck.price-map :refer [price-map initialize-price-map]]
            [bigco-pricecheck.receipt :refer [process-receipt]]
            [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(defn missing-required?
  "Check whether all options in the set required-opts are present in map options"
  [options]
  (let [required-opts #{:price-file :data-directory :output-file}]  ;; commandline options that are required on every invocation
    (not-every? options required-opts)))

(defn opts-parse [args]
  (let [;; options for commandline and their validation fns
        cli-options [["-p" "--price-file PRICEFILE" "Price data file"
                      :validate [#(and
                                   (.exists (clojure.java.io/file %))
                                   (not (.isDirectory (clojure.java.io/file %))))
                                 "Price datafile doesn't exist or is a directory"]]
                     ["-d" "--data-directory DATADIR" "Receipt data directory"
                      :validate [#(.isDirectory (clojure.java.io/file %))
                                 "Receipt data directory must be a directory"]]
                     ["-o" "--output-file OUTFILE" "Report output file"
                      :validate [#(and
                                   (not (.exists (clojure.java.io/file %)))
                                   (not (.isDirectory (clojure.java.io/file %))))
                                 "Report output file already exists or is a directory"]]]]
    (parse-opts args cli-options)))

(defn save-report
  "Generate csv string with sorted output and spit to out-file"
  [out-file store-state]
  (let [out-error-fmt (fn [e] (let [prefix (if (> e 0) "+" "")]
                                (str prefix (format "%.2f" e))))
        recs-out (->> store-state
                      (map (fn [[store-num agg-error]] (str store-num ","
                                                            (out-error-fmt agg-error))))
                      (clojure.string/join "\n"))]
    (spit out-file (str "store,plusminus\n" recs-out "\n"))))

(defn reduce-error-amounts [in-vec]
  (->> in-vec
       (mapv #(last %))
       (reduce +)))

(defn -main [& args]
  (let [{:keys [options errors summary]} (opts-parse args)]
    (cond
      (not (nil? errors)) (println
                           (str "The following parameter validation errors occurred:\n"
                                errors))
      (missing-required? options) (println
                                   (str "All of the following options are "
                                        "required for this program:\n"
                                        summary))
      :else
      (do
        (initialize-price-map (:price-file options))
        (->> options
             :data-directory
             clojure.java.io/file
             file-seq
             (filter #(not
                       (or (.isDirectory %)
                           (= (:price-file options)
                              (str %)))))
             (mapv str)
             (mapv process-receipt)
             (group-by first)
             (mapv (fn [[k v]] [k (reduce-error-amounts v)]))
             (sort-by last <)
             (save-report (:output-file options)))
        (println (str "Report saved to file "
                      (:output-file options)))))))

