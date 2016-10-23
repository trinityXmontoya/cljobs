(ns cljobs.pg
  (:require [clojure.java.jdbc :as jdbc]
            [clj-time.core :as t]
            [clj-time.coerce :as c]))

(def db-spec {:classname "org.postgresql.Driver"
             :subprotocol "postgresql"
             :subname "//localhost:5432/cljobs"})

(def tablename :job_counts)

(defn uniq-count
  [jobs]
  (println "Tallying unique jobs...")
  (let [; dup-count (count (map first (filter #(< 1 (second %)) (frequencies (map #(select-keys % [:company :title]) jobs)))))
        dupes (reduce + (remove (partial = 1) (map second (frequencies (map #(select-keys % [:company :title]) jobs)))))
        total (count jobs)]
    (println "Total jobs: " total)
    (println "Duplicate jobs: " dupes)
    (- total dupes)))

; only create if doesnt exist IF NOT EXISTS
(defn create-jobs-table
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
  (println "Writing job count to db...")
  (jdbc/insert! db-spec tablename
    [:total :date]
    [(uniq-count jobs) (c/to-sql-time (t/now))]))
