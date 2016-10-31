(ns dadysql.client
  (:require [ajax.core :as a]
            [dadysql.re-frame :as r]))


(def default-request {:method          :post
                      :headers         {}
                      :format          (a/transit-request-format)
                      :response-format (a/transit-response-format)
                      :handler         (fn [v] (js/console.log (str v)))
                      :error-handler   (fn [v] (js/console.log (str v)))})


(def csrf-headers {"Accept" "application/transit+json"
                   ;"x-csrf-token" (.-value (.getElementById js/document "csrf-token"))
                   })


(defn find-subscribe-key
  [input-request]
  (let [name (:dadysql.core/name input-request)
        group (:dadysql.core/group input-request)
        n (if (sequential? name)
            (first name)
            name)]
    (or group n)))



(defn build-request
  ([subscribe-key param-m]
   {:params        param-m
    :handler       #(r/dispatch subscribe-key %)
    :error-handler #(r/dispatch subscribe-key %)})
  ([param-m]
   (build-request (find-subscribe-key param-m) param-m)))



(defn sub-path
  [& path]
  (into [r/store-path-key] path))


(defn sub-error-path
  [& path]
  (into [r/error-path-key] path))


(defn dispatch-path [s-key v]
  [r/store-path-key [s-key v]])


(defn pull
  ([url param-m]
   (->> (build-request param-m)
        (merge default-request)
        (a/POST (str (or url "") "/pull"))))
  ([param-m]
   (pull "" param-m)))



(defn push!
  ([url param-m]
   (->> (build-request param-m)
        (merge default-request)
        (a/POST (str (or url "") "/push"))))
  ([param-m]
   (push! "" param-m)))



