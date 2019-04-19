(ns bigco-pricecheck.receipt
  (:require [bigco-pricecheck.price-map :refer [get-price]]))

(defn parse-store-num
  [store-id-line]
  (if-not (nil? store-id-line)
    (let [store-num (->> store-id-line
                         (re-matches #"[A-Z\s]+\#(\d+).+")
                         last)]
      (if-not (nil? store-num)
        (Integer/parseInt store-num)
        nil))
    nil))

(defn parse-void-item-num
  [receipt-line]
  (if-not (nil? receipt-line)
    (let [match-seq (re-matches #"\*\*\* VOIDED PRODUCT (\d+) \*\*\*\s+" receipt-line)]
      (if (not (empty? match-seq))
        (last match-seq)
        nil))
    nil))

(defn parse-item-num-receipt-price
  [receipt-line]
  (if-not (nil? receipt-line)
    (let [match-seq (re-matches #"[0-9A-Z\W\s\_]+\s+(\d+)\s+(\d+\.\d+).+" 
                                receipt-line)
          [item-nbr price-str] (rest match-seq)]
      [item-nbr (bigdec price-str)])))

(defn is-voided?
  [[top-row bottom-row]]
  (cond
    (not (nil? (parse-void-item-num top-row))) true
    (nil? bottom-row) false
    (not (nil? (parse-void-item-num bottom-row))) true
    :else false))

(defn calc-price-delta
  [[item-num receipt-price]]
  (let [item-price (bigco-pricecheck.price-map/get-price item-num)]
    (- receipt-price item-price)))

(defn crosscheck-prices
  [receipt-seq]
  (let [store-num (-> receipt-seq
                      first
                      parse-store-num)
        receipt-purchases (->> receipt-seq
                               rest
                               (partition-all 2 1)
                               (filterv #(not (is-voided? %)))
                               (mapv first))]
    (if-not (empty? receipt-purchases)
      (let [price-error (->> receipt-purchases
                             (mapv (comp calc-price-delta
                                         parse-item-num-receipt-price))
                             (reduce +))]
        [store-num, price-error])
      [store-num, 0.0M])))

(defn process-receipt
  "Reads contents of in-filename and passes to analyze-receipt-prices"
  [in-filename]
  (-> in-filename
      slurp
      clojure.string/split-lines
      drop-last
      crosscheck-prices))
