(ns cljobs.core
  (:require [cljobs.scraper :as scraper]
            [cljobs.pg :as pg]
            [cljobs.mailer :as mailer])
  (:gen-class))

(defn -main
 []
 (println "Beginning program...")
 (pg/create-job-counts-table)
 (let [jobs (scraper/master-scrape)]
    (mailer/send-jobs-email jobs)
    (pg/write-count-to-db (flatten jobs))))
