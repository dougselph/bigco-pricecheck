(ns bigco-pricecheck.price-map-test
  (:require [bigco-pricecheck.price-map :as sut :refer :all]
            [clojure.test :as t :refer [deftest testing is]]))

(deftest parse-prices-to-map-happy-path
  (with-redefs [sut/price-map (atom nil)]
    (testing "parse-prices-to-map gets expected input and initializes price data"
      (let [price-string "4651861144,104.36\n2029075259,169.05\n7975490736,144.91"]
        (is (true? (sut/parse-prices-to-map price-string)))
        (is (= (bigdec 104.36) (sut/get-price "4651861144")))
        (is (= (bigdec 169.05) (sut/get-price "2029075259")))
        (is (= (bigdec 144.91) (sut/get-price "7975490736"))))))) 

(deftest parse-prices-to-map-bad-price
  (with-redefs [sut/price-map (atom nil)]
    (testing "parse-prices-to-map returns false when price invalid for cast->float"
      (let [price-string "4651861144,104.36\n2029075259,1G9.05"]
        (is (not (sut/parse-prices-to-map price-string)))))
    (testing "parse-prices-to-map returns false when whitespace in price"
        (let [price-string "4651861144,104.36\n2029075259,1 9.05"]
          (is (not (sut/parse-prices-to-map price-string)))))
    (testing "parse-prices-to-map handles characters in product number"
        (let [price-string "4651861144,104.36\n202A9075259,169.05"]
          (is (sut/parse-prices-to-map price-string))))))


(deftest initialize-price-map-test
  (with-redefs [pricecheck.utils/file-check (constantly [true nil])
                sut/parse-prices-to-map (constantly true)
                clojure.core/slurp (constantly "")]
    (testing "A good input file will return true"
      (is (sut/initialize-price-map "some/path/prices.csv"))))
  (with-redefs [pricecheck.utils/file-check (constantly [true nil])
                sut/parse-prices-to-map (constantly false)
                clojure.core/slurp (constantly "")]
    (testing "A good input file with invalid contents will return false"
      (is (not (sut/initialize-price-map "some/path/prices.csv")))))
  (with-redefs [pricecheck.utils/file-check (constantly [false :not-found])]
    (testing "A missing input file will return false"
      (is (not (sut/initialize-price-map "some/path/prices.csv")))))
  (with-redefs [pricecheck.utils/file-check (constantly [false :directory])]
    (testing "An input file path which is a directory will return false"
      (is (not (sut/initialize-price-map "some/path/prices.csv"))))))
