(ns cljobs.pg
  (:require [clojure.java.jdbc :as jdbc]
            [clj-time.core :as t]
            [clj-time.coerce :as c]))

(def db-spec {:classname "org.postgresql.Driver"
             :subprotocol "postgresql"
             :subname "//localhost:5432/cljobs"})  ;; Good to use env here too to adapt to the deployment 
                                                   ;; ex. (or (env :database_url) "//localhost:5432/cljobs")

(def tablename :job_counts)

(defn uniq-count
  [jobs]
  (println "Tallying unique jobs...")
  (let [dupes (->> (map #(select-keys % [:company :title]) jobs) ;; glad to see threading macro here, I used them often
                   frequencies                                   ;; would be cleaner to write, so you know what your threading:
                                                                 ;; (->> jobs
                   (map second)                                  ;;   (map #(select-keys % [:company :title])) ...
                   (remove #{1})
                   (reduce +))
        total (count jobs)]
    (println "Total jobs: " total)
    (println "Duplicate jobs: " dupes)
    (- total dupes)))

(defn create-job-counts-table
  []
  (println "Creating job_counts table...")
  (jdbc/db-do-commands db-spec
    (jdbc/create-table-ddl tablename
      [[:id "serial" "PRIMARY KEY"]
      [:total "int"]
      [:date "timestamp"]]))
  (println "job_counts table created"))

(defn write-count-to-db
  [jobs]
  (let [total (uniq-count jobs)]
    (println "Writing job count (" total ") to db...")
    (jdbc/insert! db-spec tablename
      [:total :date]
      [total (c/to-sql-time (t/now))])
    (println "Write to database complete.")))
