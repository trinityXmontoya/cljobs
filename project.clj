(defproject cljobs "0.1.0-SNAPSHOT"
  :description "See README"
  :url "https://github.com/trinityXmontoya/cljobs.git"
  :license {:name "Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International"
            :url "https://creativecommons.org/licenses/by-nc-sa/4.0/legalcode"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 ; postgres
                 [org.clojure/java.jdbc "0.6.2-alpha3"]
                 [org.postgresql/postgresql "9.4-1201-jdbc41"]
                 ;mailer
                 [com.draines/postal "2.0.0"]
                 ;http
                 [clj-http "2.3.0"]
                 [http-kit "2.2.0"]
                 ;parser / formatter
                 [cheshire "5.6.3"]
                 [enlive "1.1.6"]
                 [org.clojure/data.xml "0.0.8"]
                 [clj-time "0.12.0"]
                 ; env vars
                 [environ "1.1.0"]
                 ]
  :plugins [[lein-environ "1.1.0"]])
