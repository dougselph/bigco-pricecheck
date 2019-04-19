(ns bigco-pricecheck.price-map
  (:require [bigco-pricecheck.utils :refer [file-check]]))

(def price-map (atom nil))

(defn get-price-for-memoization
  "Look up price for item-nbr received in price-map atom"
  [item-nbr]
  (@price-map item-nbr))

;; price-map is a small, frequently queried dataset,
;; so memoize the price lookup function as get-price
;; for faster price lookups.
;; At scale, this would almost certainly need to be
;; revisited.
(def get-price (memoize get-price-for-memoization))

(defn split-product-prices-to-vecs
  [lines-seq]
  (try
    (mapv #(let [[k v] (clojure.string/split % #",")]
             [k (bigdec v)]) lines-seq)
    (catch java.lang.NumberFormatException e
      (do
        (println (str "Invalid price encountered: " (.getMessage e)))
        []))
    (catch Exception e
      (do
        (println (str "An error occured while loading price data: "
                      (.getMessage e)))
        []))))

(defn parse-prices-to-map
  "Price data file contents passed as string. Parse to a map and reset!
  into price-map atom"
  [in-str]
  (let [temp-map (->> in-str
                      clojure.string/split-lines
                      split-product-prices-to-vecs
                      (into {}))]
    (if (and (map? temp-map)
             (not (empty? temp-map))
             (every? (fn [[k v]] (and (decimal? v)
                                      (string? k))) temp-map))
      (do (reset! price-map temp-map)
          true)
      (do (println "There was an error loading pricing data from input file.")
          false))))

(defn initialize-price-map
  "Check that file at path in-file exists and is not a directory,
  then call parse-prices-to-map to initialize price dataset for
  analysis of sales receipts."
  [in-file]
  (let [[good-file? cause] (file-check in-file)]
    (if good-file?
      (-> in-file
          slurp
          parse-prices-to-map)
      (do
        (condp = cause
          :directory (println "Price data file name is a directory")
          (println "Price data file not found."))
        false))))

