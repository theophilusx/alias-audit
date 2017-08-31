(defproject alias-audit "0.1.0-SNAPSHOT"
  :description "Purpose specific tool written to help solve a specific problem" 
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.csv "0.1.4"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/tools.logging "0.4.0"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [cprop "0.1.11"]
                 [org.clojars.zentrope/ojdbc "11.2.0.3.0"]
                 [com.mchange/c3p0 "0.9.5.2"]
                 [org.clojure/java.jdbc "0.7.1"]
                 [com.layerware/hugsql "0.4.7"]]
  :main ^:skip-aot alias-audit.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
