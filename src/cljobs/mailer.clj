(ns cljobs.mailer
  (:require [environ.core :refer [env]]
            [net.cgrand.enlive-html :as html]
            [postal.core :as postal]))

(def host-config
  {:host "smtp.gmail.com"
   :user (:google-email env)
   :pass (:google-pw env)
   :ssl true})

(def msg-config
  {:from (:google-email env)
   :to (:recipient-email env)
   :subject "Clojure Jobs"})

(html/defsnippet listing-snippet "email-template.html"
  {[:h2] [[:div.post-body (html/nth-of-type 1)]]}
  [jobs]
  [:h2] (html/content (str (:source (first jobs)) ": " (count jobs)))
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
        msg-config (merge msg-config {:body [{:type "text/html"
                                              :content body}]})]
    (println "Sending jobs email to" (:recipient-email env))
    (postal/send-message host-config msg-config)))
