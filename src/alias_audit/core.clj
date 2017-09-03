(ns alias-audit.core
  (:require [clojure.string :as s]
            [clojure.set :as set]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.tools.logging :as log]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [alias-audit.config :refer [config]]
            [alias-audit.urs :as urs]
            [alias-audit.utils :as u])
  (:gen-class))

(defn parse-args [options arguments]
  (reduce merge {} (map #(conj [] %1 %2) (keys options) arguments)))

(defn process-distribution-lists [f]
  (with-open [rdr (io/reader f)]
    (set (reduce (fn [d-lst csv-rec]
                   (if (empty? csv-rec)
                     d-lst
                     (conj d-lst (s/lower-case (s/trim (first (s/split (second csv-rec) #"@")))))))
                 [] (csv/read-csv rdr)))))

(defn process-personal-alias-list [f]
  (with-open [rdr (io/reader f)]
    (set (reduce (fn [a-list csv-rec]
                   (if (empty? csv-rec)
                     a-list
                     (conj a-list (s/lower-case (s/trim (first (s/split (nth csv-rec 2) #"@")))))))
                 [] (csv/read-csv rdr)))))

(defn parse-postfix-alias [a]
  (let [[alias-name recipient-list] (s/split a #"\:")
        recipients (mapv #(s/lower-case (s/trim %)) (s/split recipient-list #"\,"))]
    [(s/lower-case (s/trim alias-name)) recipients]))

(defn process-postfix-aliases [f]
  (with-open [rdr (io/reader f)]
    (reduce (fn [m l]
              (cond
                (s/starts-with? l "#") m
                (s/blank? l) m
                :else (let [[alias-name recipients] (parse-postfix-alias l)]
                        (assoc m alias-name recipients))))
            {} (line-seq rdr))))

(defn filter-exchange-aliases [postfix-aliases exchange-aliases]
  (filter #(contains? exchange-aliases %) (keys postfix-aliases)))

(defn sort-aliases [alias-m]
  (reduce (fn [m a]
            (let [rcpts (get alias-m a)]
              (if (= 1 (count rcpts))
                (update-in m [:single-rcpt] conj a)
                (update-in m [:multi-rcpt] conj a))))
          {:single-rcpt [] :multi-rcpt []} (keys alias-m)))


(defn process-data [options arguments]
  (let [input-files (parse-args options arguments)
        dist-list (process-distribution-lists (:distribution-lists input-files))
        personal-list (process-personal-alias-list (:personal-aliases input-files))
        all-exchange (set/union dist-list personal-list)
        postfix-aliases (process-postfix-aliases (:postfix-aliases input-files))
        in-both (filter-exchange-aliases postfix-aliases all-exchange)
        pfix-aliases-2 (u/remove-keys postfix-aliases in-both)
        urs-alias-status (urs/get-urs-alias-status (keys pfix-aliases-2))
        pfix-aliases-3 (u/remove-keys
                        (u/remove-keys pfix-aliases-2 (:inactive urs-alias-status))
                        (:unknown urs-alias-status))
        sorted-aliases (sort-aliases pfix-aliases-3)
        dead-single-rcpt (urs/find-inactive-rcpt pfix-aliases-3 (:single-rcpt sorted-aliases))]

    (println (str "\nDuplicates To Remove From Postfix Aliases File"))
    (doseq [a in-both]
      (println (str "\t" a)))

    (println (str "\nInactive Aliases To Remove From Posttrix Alias File"))
    (doseq [a (:inactive urs-alias-status)]
      (println (str "\t" a)))

    (println (str "\nUnknown Aliases To Remove From Postfix Alias File"))
    (doseq [a (:unknown urs-alias-status)]
      (println (str "\t" a)))

    (println (str "\nAliases With Inactive Recipeint:"))
    (doseq [a (:remove dead-single-rcpt)]
      (println (str "\t" a)))

    (println (str "\nAliases With Unknown Recipeint: "))
    (doseq [a (:unknown dead-single-rcpt)]
      (println (str "\t" a)))
    
    (println (str "\n"))
    (println (str "\nDistribution Lists: " (count dist-list)))
    (println (str "Personal Aliases: " (count personal-list)))
    (println (str "Total Unique: " (count all-exchange)))
    (println (str "Total Postfix Aliases: " (count (keys postfix-aliases))))
    (println (str "Total To Remove From Postfix: " (count in-both)))
    (println (str "Remaining to Check 1: " (count (keys pfix-aliases-2))))
    (println (str "Inactive Aliases: " (count (:inactive urs-alias-status))))
    (println (str "Unknown Aliases: " (count (:unknown urs-alias-status))))
    (println (str "Remaining To Check 2: " (count (keys pfix-aliases-3))))
    (println (str "Single Recipient Aliases: " (count (:single-rcpt sorted-aliases))))
    (println (str "Group Aliases: " (count (:multi-rcpt sorted-aliases))))
    (println (str "Aliases With Inactive Recipient: " (count (:remove dead-single-rcpt))))
    (println (str "Aliases With Unknown Recipeint: " (count (:unknown dead-single-rcpt))))))

;; Command line processing

(def cli-options [["-a" "--postfix-aliases" "The Postfix Aliases File"]
                  ["-d" "--distribution-lists" "Exchange Distribution Lists CSV"]
                  ["-p" "--personal-aliases" "Exchange Personal Aliases CSV"]
                  ["-h" "--help"]])

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn error-msg [errors]
  (str "The following errors occurred while parsing the options\n\n"
       (s/join \newline errors)))

(defn usage [option-summary]
  (let [app-name (config :app-name)
        app-version (config :app-version)]
    (s/join
     \newline
     [(str app-name "(version " app-version ")")
      ""
      (str "Usage: " app-name " -a <file> -d <file> -p <file>")
      ""
      "Options:"
      option-summary])))

(defn -main
  "Main entry function" 
  [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) (exit 0 (usage summary))
      (not= 3 (count arguments)) (exit 1 (str "Error: Missing arguments\n\n"
                                              (usage summary)))
      errors (exit 1 (error-msg errors))
      :default (process-data options arguments))))

