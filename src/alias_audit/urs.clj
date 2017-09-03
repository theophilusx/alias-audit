(ns alias-audit.urs
  (:require [alias-audit.config :refer [config]]
            [clojure.tools.logging :as log]
            [hugsql.core :refer [def-db-fns]])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

(def db-spec {:classname "oracle.jdbc.driver.OracleDriver"
              :subprotocol "oracle:thin"
              :subname (config :db-name)
              :user (config :db-user)
              :password (config :db-pwd)
              :initial-pool-size 3
              :min-pool-size 3
              :max-pool-size 15})

(defn pool [spec]
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass (:classname spec))
               (.setJdbcUrl (str "jdbc:"
                                 (:subprotocol spec) ":"
                                 (:subname spec)))
               (.setUser (:user spec))
               (.setPassword (:password spec))
               (.setMinPoolSize (:min-pool-size spec 3))
               (.setMaxPoolSize (:max-pool-size spec 15))
               (.setInitialPoolSize (:initial-pool-size spec 0))
               (.setCheckoutTimeout (:max-wait spec 0))
               (.setMaxConnectionAge (quot (:max-connection-lifetime spec 3600000) 1000))
               (.setMaxIdleTime (quot (:max-connection-idle-lifetime spec 1800000) 1000))
               (.setMaxIdleTimeExcessConnections 120)
               (.setMaxStatements 180)
               ;; Connection testing
               (.setPreferredTestQuery (:test-connection-query spec nil))
               (.setTestConnectionOnCheckin (:test-connection-on-borrow spec false))
               (.setTestConnectionOnCheckout (:test-connection-on-return spec false))
               ;; Connection testing
               (.setPreferredTestQuery (:test-connection-query spec nil))
               (.setTestConnectionOnCheckin (:test-connection-on-borrow spec false))
               (.setTestConnectionOnCheckout (:test-connection-on-return spec false))
               (.setIdleConnectionTestPeriod (quot
                                              (:test-idle-connections-period spec 800000)
                                              1000)))]
    {:datasource cpds}))

(def db-pool (delay (pool db-spec)))

(def-db-fns "sql/queries.sql")

