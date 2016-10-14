(ns dadysql.re-frame
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as r]))

(def error-path :dadysql/error-path)

(defn sub-path
  ([] [:dadysql/store] )
  ([p] [:dadysql/store p]))


(defn store-path [k v]
  [:dadysql/store [k v]])


(defn clear-path
  ([]  [:dadysql/clear-store] )
  ([k] [:dadysql/clear-store k]))


;; Clear all value from here
(r/register-handler
  :dadysql/clear-store
  (fn [db [_ v]]
    (if v
      (update-in db [:dadysql/store ] dissoc v)
      (assoc-in db [:dadysql/store] nil))))


;;Store all value here
(r/register-handler
  :dadysql/store
  (fn [db [p [cp v]]]
    (assoc-in db [p cp] v)))


(r/register-sub
  :dadysql/store
  (fn [db path] (do (reaction (get-in @db path)))))


(defn find-subscribe-key
  [input-request]
  (let [name (:dadysql.core/name input-request)
        group (:dadysql.core/group input-request)
        n (if (sequential? name)
            (first name)
            name)]
    (or group n)))



(defn- as-dispatch
  [subscribe-key]
  (fn [[v e]]
    (if v
      (do
        (r/dispatch (clear-path error-path) )
        (r/dispatch [:dadysql/store [subscribe-key v]]))
      (r/dispatch [:dadysql/store [:dadysql/error-path {subscribe-key e}]]))))





(defn build-request
  ([subscribe-key param-m]
   (let []
     {:params        param-m
      :handler       (as-dispatch subscribe-key)
      :error-handler (as-dispatch subscribe-key)}))
  ([param-m]
   (build-request (find-subscribe-key param-m) param-m)))
