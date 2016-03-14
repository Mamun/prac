(ns tiesql.client
  (:require [ajax.core :as a]
            [tiesql.common :as c]
            [ring.middleware.tiesql-util :as u]
            [cljs.core.async :refer [<! >! timeout chan]])
  (:require-macros [cljs.core.async.macros :refer [go]]))


(def accept-html-options
  {:accept "text/html"
   :input  :string
   :output :string})


(defn response-format
  [request-m]
  (condp = (get-in request-m [:accept])
    "text/html" (a/text-request-format)
    (a/transit-response-format)))


(defn warp-accept-callback
  [callback {:keys [accept]}]
  (if (= "text/html" accept)
    (fn [res]
      (callback (:original-text res)))
    callback))


(defmulti do-ajax (fn [_ {:keys [callback]}]
                    (when callback
                      :callback)))


(def csrf-headers {"Accept"       "application/transit+json"
                   ;"x-csrf-token" (.-value (.getElementById js/document "csrf-token"))
                   })

(defmethod do-ajax :callback
  [url {:keys [callback headers] :as request-m}]
  (let [w (u/validate-and-assoc-default request-m)
        headers (or headers {})]
    (if (c/failed? w)
      (callback (u/response-format w))
      (let [cb (warp-accept-callback callback request-m)]
        (a/POST url {:response-format (response-format request-m)
                     :request-format  (a/transit-request-format)
                     :params          w
                     :handler         cb
                     :error-handler   cb
                     :headers         headers}
                )))))


(defmethod do-ajax :default
  [url request-m]
  (let [ch (chan 1)
        callback (fn [v] (go (>! ch v)))]
    (go
      (->> (assoc request-m :callback callback)
           (do-ajax url)))
    ch))


(defn pull [& {:keys [url] :as request-m}]
  (do-ajax (str (or url "") "/pull") request-m))


(defn push!
  [& {:keys [url] :as request-m}]
  (do-ajax (str (or url "") "/push") request-m))


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
            :callback cb} options)))


(def ^:export h-options (clj->js accept-html-options))


#_(defn ^:export pull1
    ([url name params options callback]
     (->> (build-js-request name params options callback)
          (call-http-service (str url "/pull"))))
    ([url name params callback]
     (pull1 url name params h-options callback)))


#_(defn ^:export push
    [url name params callback]
    (->> (build-js-request name params {} callback)
         (call-http-service (str url "/push"))))


