(ns bigco-pricecheck.receipt-test
  (:require  [clojure.test :as t :refer [deftest testing is use-fixtures]]
             [bigco-pricecheck.receipt :as sut]))

(def sample-file-contents (str "BIGCO  STORE #2    \n"
                               "STOLEN-CREDIT-CARD   2925648759       40.25  \n"
                               "*** VOIDED PRODUCT 2925648759 ***            \n"
                               "DAVID NOLENS HAIR   5329799687        33.72  \n"
                               "JET FUEL   7975490736                144.91  \n"
                               "YOUTUBE-CELEBRITY   5449673245       138.13 X\n"
                               "*** VOIDED PRODUCT 5449673245 ***            \n"
                               "                                TOTAL 3690.09"))

(defn pricing-fixture [f]
  (pricecheck.price-map/initialize-price-map "resources/data/prices.csv")
  (f)
  (reset! pricecheck.price-map/price-map nil))

(use-fixtures :once pricing-fixture)


(deftest process-receipt-test
  (with-redefs [pricecheck.utils/file-check (constantly [true nil])
                clojure.core/slurp (constantly sample-file-contents)
                sut/crosscheck-prices (constantly [2 (bigdec 0)])]
    (testing "A good receipt file will return true"
      (is (sut/process-receipt "some/path/filename")))))


(deftest parse-store-num-test
  (testing "Parse the store number from the first line of receipt file"
    (is (= 2 (sut/parse-store-num "BIGCO  STORE #2      ")))
    (is (nil? (sut/parse-store-num "DAVID NOLENS HAIR   4358837482     33.72  ")))
    (is (nil? (sut/parse-store-num nil)))))


(deftest parse-void-item-num-test
  (testing "Parse the item number from \"VOIDED PRODUCT\" rows"
    (is (= "5449673245" (sut/parse-void-item-num (str "*** VOIDED PRODUCT"
                                                      " 5449673245 ***           "))))
    (is (nil? (sut/parse-void-item-num "DAVID NOLENS HAIR   4358837482     33.72  ")))
    (is (nil? (sut/parse-void-item-num nil)))))


(deftest parse-item-num-receipt-price
  (testing "Parse the item number (as string) and receipt price from a receitpt's product row"
    (is (= ["4358837482" (bigdec 33.72)] (sut/parse-item-num-receipt-price "DAVID NOLENS HAIR   4358837482     33.72  ")))))


(deftest is-voided?-test
  (testing "is-voided? detects receipt row couplets which should be filtered out as voided and not included in reduction of net pricing error"
    (is (sut/is-voided? ["STOLEN-CREDIT-CARD   2925648759       40.25  "
                         "*** VOIDED PRODUCT 2925648759 ***            "]))

    (is (sut/is-voided? ["*** VOIDED PRODUCT 2925648759 ***            "
                         "STOLEN-CREDIT-CARD   2925648759       40.25  "]))

    (is (not (sut/is-voided? ["STOLEN-CREDIT-CARD   2925648759       40.25  "
                              nil])))

    (is (not (sut/is-voided? ["STOLEN-CREDIT-CARD   2925648759       40.25  "
                              ""])))

    (is (not (sut/is-voided? ["STOLEN-CREDIT-CARD   2925648759       40.25  "
                              "DAVID NOLENS HAIR   5329799687        33.72  "])))))


(deftest calc-price-delta-test
  (with-redefs [pricecheck.price-map/get-price (fn [x]
                                                 ({"5329799687" (bigdec 33.72)
                                                   "7975490736" (bigdec 144.91)} x))]
    (testing "Calculated price delta is 0.00 when correct price is charged"
      (is (= (bigdec 0) (sut/calc-price-delta ["5329799687" (bigdec 33.72)]))))
    (testing "Calculated price delta is 10.00 when incorrect, higher price is charged"
      (is (= (bigdec 10) (sut/calc-price-delta ["5329799687" (bigdec 43.72)]))))
    (testing "Calculated price delta is -10.00 when incorrect, lower price is charged"
      (is (>= (bigdec -10)
              (sut/calc-price-delta ["5329799687" (bigdec 23.72)])
              (bigdec -10.001))))))


(deftest crosscheck-prices-test
  (with-redefs [pricecheck.price-map/get-price (fn [x]
                                                 ({"5329799687" (bigdec 33.72)
                                                   "7975490736" (bigdec 144.91)} x))]
    (testing "crosscheck-prices returns a zero price-error when prices of non-voided items match pricing input data"
      (let [[store-num price-error] (sut/crosscheck-prices
                                     (drop-last
                                      (clojure.string/split-lines sample-file-contents)))]
        (is (= 2 store-num))
        (is (= (bigdec 0) price-error)))))
  (with-redefs [pricecheck.price-map/get-price (fn [x]
                                                 ({"5329799687" (bigdec 23.72)
                                                   "7975490736" (bigdec 44.91)} x))]
    (testing "crosscheck-prices returns expected, correct positive price-error when prices of non-voided items are above those listed in pricing input data"
      (let [[store-num price-error] (sut/crosscheck-prices
                                     (drop-last
                                      (clojure.string/split-lines sample-file-contents)))]
        (is (= 2 store-num))
        (is (<= (bigdec 110) price-error (bigdec 110.009))))))
  (with-redefs [pricecheck.price-map/get-price (fn [x]
                                                 ({"5329799687" (bigdec 43.72)
                                                   "7975490736" (bigdec 244.91)} x))]
    (testing "crosscheck-prices returns expected, correct negative price-error when prices of non-voided items are below those listed in pricing input data"
      (let [[store-num price-error] (sut/crosscheck-prices
                                     (drop-last
                                      (clojure.string/split-lines sample-file-contents)))]
        (is (= 2 store-num))
        (is (= (bigdec -110) price-error))))))

