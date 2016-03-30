(ns tiesql.client
  #?(:clj
     (:require [ajax.core :as a]
               [tiesql.common :as c]
               [tiesql.util :as u]
               [clojure.core.async :refer [<! >! timeout chan go]]))
  #?@(:cljs
      [(:require [ajax.core :as a]
         [tiesql.common :as c]
         [tiesql.util :as u]
         [cljs.core.async :refer [<! >! timeout chan]])
       (:require-macros [cljs.core.async.macros :refer [go]])]))


(def default-request-format
  {:method          :post
   :format          (a/transit-request-format)
   :response-format (a/transit-response-format)})



(def accept-html-options
  {:accept "text/html"
   :input  :string
   :output :string})


(defmulti do-ajax-request (fn [_ {:keys [callback]}]
                            (when callback
                              :callback)))


(def csrf-headers {"Accept" "application/transit+json"
                   ;"x-csrf-token" (.-value (.getElementById js/document "csrf-token"))
                   })

(defmethod do-ajax-request :callback
  [url {:keys [callback headers] :as request-m}]
  (let [w (u/validate-and-assoc-default request-m)
        headers (or headers {})]
    (if (c/failed? w)
      (callback (u/response-format w))
      (-> default-request-format
          (assoc :uri url)
          (assoc :params w)
          (assoc :headers headers)
          (assoc :handler (fn [[_ v]] (callback v)))
          (a/ajax-request)))))



(defmethod do-ajax-request :default
  [url request-m]
  (let [ch (chan 1)
        callback (fn [v] (go (>! ch v)))]
    (go
      (->> (assoc request-m :callback callback)
           (do-ajax-request url)))
    ch))


(defn pull [& {:keys [url] :as request-m}]
  (do-ajax-request (str (or url "") "/pull") request-m))


(defn push!
  [& {:keys [url] :as request-m}]
  (do-ajax-request (str (or url "") "/push") request-m))


#?(:cljs
   (defn build-js-request
     [name params options callback]
     (let [namev (mapv keyword (js->clj name))
           params (js->clj params)
           options (js->clj options)
           cb (fn [res] (if (= "text/html" (get-in options [:accept]))
                          (callback res)
                          (callback (clj->js res))))]
       (merge {:name     namev
               :params   params
               :input    :string
               :output   :string
               :callback cb} options))))


#_(def ^:export h-options (clj->js accept-html-options))


(comment

  (let [v (pull
            :url "http://localhost:3001"
            :name :get-dept-by-id
            :params {:id 1}
            :callback (fn [w]
                        (clojure.pprint/pprint w)
                        ))]
    (println @v)
    )

  )