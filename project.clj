(defproject alias-audit "0.1.0-SNAPSHOT"
  :description "Purpose specific tool written to help solve a specific problem" 
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.csv "0.1.4"]
                 [org.clojure/tools.cli "0.3.5"]
                 [cprop "0.1.11"]]
  :main ^:skip-aot alias-audit.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
