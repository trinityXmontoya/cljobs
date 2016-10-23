(ns cljobs.mailer
  (:require [environ.core :refer [env]]
            [net.cgrand.enlive-html :as html]
            [postal.core :as postal]))

(def host-config
  {:host "smtp.gmail.com"
   :user (env :google-email)
   :pass (env :google-pw)
   :ssl true})

(def msg-config
  {:from (env :google-email)
   :to (env :recipient-email)
   :subject "Clojure Jobs"})

(html/defsnippet listing-snippet "email-template.html"
  {[:h2] [[:div.post-body (html/nth-of-type 1)]]}
  [jobs]
  [:h2] (html/content (str ((first jobs) :source) ":" (count jobs)))
  [:ul [:li html/first-of-type]] (html/clone-for [{:keys [company title location link]} jobs]
                                                 [:li :a] (html/content (str company " - " title " - " location))
                                                 [:li :a] (html/set-attr :href link)))

(html/deftemplate email-template "email-template.html"
  [jobs]
  [:title] (html/content "Clojure Jobs")
  [:body] (html/content (map listing-snippet jobs)))

(defn build-jobs-email
  [jobs]
  (reduce str (email-template jobs)))

(defn send-jobs-email
  [jobs]
  (let [body (build-jobs-email jobs)
        msg-config (merge mailer-msg-config {:body [{:type "text/html"
                                                     :content body}]})]
    (postal/send-message host-config
                         msg-config)))
