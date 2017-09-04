(ns alias-audit.urs
  (:require [alias-audit.config :refer [config]]
            [clojure.string :as string]
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

(def urs-id-cache (atom {}))

(defn urs-id-lookup [id]
  (if (contains? @urs-id-cache id)
    (get @urs-id-cache id)
    (let [urs-rec (get-identity-status @db-pool id)
          status (if urs-rec
                   {:status (:status urs-rec)
                    :category (:id_category urs-rec)}
                   {:status "Unknown"
                    :category "Unknown"})]
      (swap! urs-id-cache #(assoc % id status))
      status)))

(defn get-urs-alias-status [as]
  (reduce (fn [status-m a]
            (let [alias-rec (get-alias @db-pool {:alias a})]
              (cond
                (nil? alias-rec) (update-in status-m [:unknown] conj a)
                (= "Inactive" (:status alias-rec)) (update-in status-m [:inactive] conj a)
                (= "Active" (:status alias-rec)) status-m
                :default (println (str "Bad data: Alias=" a " Status=" (:status alias-rec))))))
          {:inactive [] :unknown []} as))

(defn strip-une-domain [rcpt]
  (if (or (string/includes? rcpt "@une.edu.au")
          (string/includes? rcpt "@ad.une.edu.au"))
    (first (string/split rcpt #"@"))
    rcpt))

(defn recipient-status [rcpt]
  (let [urs-rec (get-identity-status @db-pool {:id (strip-une-domain rcpt)})]
    (cond
      (nil? urs-rec) :unknown
      (= "Alias" (:category urs-rec)) (let [a-rec (get-urs-alias-status @db-pool rcpt)]
                                        (cond
                                          (nil? a-rec) :unknown
                                          (= "Inactive" (:status a-rec)) :inactive
                                          :default :active))
      (= "Reserved" (:category urs-rec)) :inactive
      (= "Inactive" (:status urs-rec)) :inactive
      :default :active)))

(defn find-inactive-rcpt [alias-m alias-l]
  (reduce (fn [m a]
            (let [rcpt (first (get alias-m a))
                  status (recipient-status rcpt)]
              (condp = status
                :inactive (update-in m [:remove] conj a)
                :unknown (update-in m [:unknown] conj a)
                :active m)))
          {:unknown [] :remove []} alias-l))

(defn group-with-inactive-rcpt [alias-m alias-l]
  (reduce (fn [m a]
            (let [rcpts (get alias-m a)
                  dead-rcpts (reduce (fn [v r]
                                       (if (= :inactive (recipient-status r))
                                         (conj v r)
                                         v))
                                     [] rcpts)]
              (if (empty? dead-rcpts)
                m
                (assoc m a dead-rcpts))))
          {} alias-l))
