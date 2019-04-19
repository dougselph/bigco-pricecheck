(defproject bigco-pricecheck "1.0.1"
  :description "Report on aggregate pricing errors for BIGCO"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/tools.cli "0.4.2"]]
  :main bigco-pricecheck.core
  :profiles {:uberjar {:aot :all}}
  :target-path "target/%s"
  :min-lein-version "2.8.1")
