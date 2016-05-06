(ns app.html-routes
  (:use [selmer.parser]
        [compojure.core])
  (:require [clojure.java.io :as io]
            [compojure.route :as route]))


(set-resource-path! (clojure.java.io/resource "public"))


(defn html-response
  [body]
  {:status  200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body    body})


#_(html-response (io/input-stream (io/resource "public/index.html")))


(defn index []
  (html-response
    (render-file "index.html" {:title "index"
                                :navs [{:link "index" :text "Home" :is-active "is-active"}
                                       {:link "contact" :text "Contact"}]})))

(defn contact []
  (html-response
    (render-file "contact.html" {:title "Contact"
                                 :navs [{:link "index" :text "Home"}
                                        {:link "contact" :text "Contact" :is-active "is-active"}]})))




(defroutes
  html-page-routes
  (GET "/" _ (index) )
  (GET "/index" _ (index))
  (GET "/contact" _ (contact)))