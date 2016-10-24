(ns cljobs.scraper
  (:require [clj-http.client :as client]
            [cheshire.core :as json]
            [environ.core :refer [env]]
            [clojure.data.xml :as xml]
            [clojure.string :as string]
            [net.cgrand.enlive-html :as html]
            [clj-time.core :as t]
            [clj-time.coerce :as c]))

; ----------------------------------
; HELPERS --------------------------
; ----------------------------------
(def lang "clojure")

; update schema for minor changes as needed
(def scrape-schemas
  {:stackoverflow {:chunk 25
                   :url "http://stackoverflow.com/jobs"
                   :query-params {:searchTerm lang}
                   :pg-selector [:span.description]}
   :monster {:chunk 35
             :url "https://www.monster.com/jobs/search"
             :query-params {:q lang :isDynamicPage true}
             :pg-selector [:h2.page-title]}
   :indeed {:chunk 25
            :url "http://api.indeed.com/ads/apisearch"
            :query-params {:v 2 :q lang :limit 25 :publisher (:indeed-api-key env)}}})

(defn compact
  [collection]
  (remove nil? (flatten collection)))

(defn parse-int [s]
  "string->int
   src: http://stackoverflow.com/a/12503724/3481754"
   (Integer. (re-find  #"\d+" s)))

(defn get-body
  [url query-params]
  (:body (client/get url {:query-params query-params})))

(defn get-text
  [doc selector]
  (-> (html/select doc selector) first html/text string/trim))

(defn get-attr
  [doc selector attr]
  (-> (html/select doc selector) first (get-in [:attrs (keyword attr)])))

(defn get-elements
  [url query-params selector]
  (let [doc (get-body url query-params)]
    (html/select (html/html-snippet doc) selector)))

(defn get-num-pgs
  "return total number of pages based off string total records and page chunk"
  [records chunk]
   (-> (parse-int records) (/ chunk) Math/ceil int))

(defn get-number
  "retrieve number in the text of doc at a given selector"
  [doc selector]
  (re-find #"\d+" (get-text (html/html-snippet doc) selector)))

(defn paginated-scrape
  "perform given handler-fn for each page of the given schema"
  [schema handler-fn]
  (let [{:keys [url query-params chunk pg-selector]} (scrape-schemas schema)
        doc (get-body url query-params)
        num-jobs (get-number doc pg-selector)
        pages (get-num-pgs num-jobs chunk)]
    (map handler-fn (range pages))))

; ----------------------------------
; SCRAPERS -------------------------
; ----------------------------------
(defn so-scrape
  []
  (println "Scraping stackoverflow...")
  (let [{:keys [url query-params]} (scrape-schemas :stackoverflow)]
    (paginated-scrape
      :stackoverflow
      (fn [pg-num]
        (let [query-params (merge query-params {:pg pg-num})
              listings (get-elements url query-params [:.-job])]
          (map
            (fn [listing]
              {:source "Stack Overflow"
               :company (get-text listing [:li.employer])
               :title (get-text listing [:a.job-link])
               :location (get-text listing [:li.location])
               :link (->> listing
                          (#(get-attr % [:a.job-link] :href))
                          (re-find #"(.+)\?")
                          second
                          (str "http://stackoverflow.com"))})
            listings))))))

(defn monster-scrape
  []
  (println "Scraping monster...")
  (let [{:keys [url query-params]} (scrape-schemas :monster)
        search-url (str url "/pagination")]
    (paginated-scrape
      :monster
      (fn [pg-num]
        (let [query-params (merge query-params {:page (inc pg-num)})
              listings (json/decode (get-body search-url query-params))]
          (map
            (fn [listing]
              {:source "Monster"
               :company (get-in listing ["Company" "Name"])
               :title (listing "Title")
               :location (listing "LocationText")
               :link (listing "JobViewUrl")})
            listings))))))

(defn indeed-scrape
  []
  (println "Scraping indeed...")
  (let [{:keys [url query-params chunk]} (scrape-schemas :indeed)
        doc (get-body url query-params)
        num-jobs (->> (java.io.StringReader. doc)
                      xml/parse
                      .content
                      (filter #(= (.tag %) :totalresults))
                      first
                      .content
                      first)
        pages (get-num-pgs num-jobs chunk)]
    (for [p (range pages)]
      (let [query-params (merge query-params {:start (* p chunk)})
            doc (get-body url query-params)
            listings (->> (java.io.StringReader. doc)
                          xml/parse
                          .content
                          (filter #(= (.tag %) :results))
                          first
                          .content)]
            (map
              (fn [listing]
                (let [res-map (->> (.content listing)
                                   (map #(hash-map (.tag %)
                                                   (first (.content %))))
                                   (into {}))]
                  {:source "Indeed"
                   :company (:company res-map)
                   :title (:jobtitle res-map)
                   :location (:formattedLocation res-map)
                   :link (:url res-map)}))
              listings)))))

(defn master-scrape
  []
  (let [listings [(so-scrape) (indeed-scrape) (monster-scrape)]]
    (map compact listings)))
