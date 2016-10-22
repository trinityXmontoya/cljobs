(ns cljobs.core
  (:require [clj-http.client :as client]
            [cheshire.core :as json]))
;

(def lang "clojure")

(defn scrape
  []
  (let [id-url "https://angel.co/job_listings/startup_ids"
        id-res (json/decode ((client/get id-url {:query-params {"filter_data[keywords][]" lang}}) :body))
        job-url "https://angel.co/job_listings/browse_startups_table"
        listing-id-params (clojure.string/join "&" (map-indexed (fn [idx id] (str "listing_ids[" idx "][]=" (first id))) (id-res "listing_ids")))
        job-url (str job-url "?" listing-id-params)
        jobs-doc ((client/get job-url {:query-params {:multi-param-array :array "startup_ids" (id-res "ids")}}) :body)
        job-listings (html/select (html/html-snippet jobs-doc) [:div.browse_startups_table_row])]
    (map
      (fn [listing]
          {:company (html/text (first (html/select listing [:a.startup-link])))
           :tagline (html/text (first (html/select listing [:div.tagline])))
           :title (html/text (first (html/select listing [:div.collapsed-title])))
           :location (html/text (first (html/select listing [:div.locations])))
           :link (get-in (first (html/select listing [:div.details :div.title :a])) [:attrs :href])
           :compensation (html/text (first (html/select listing [:div.compensation])))
           :source :angel-list})
        job-listings)))
