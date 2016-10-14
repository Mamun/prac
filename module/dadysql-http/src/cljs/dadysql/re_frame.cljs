(ns dadysql.re-frame
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as r]))


(defn get-path
  ([] [:dadysql/path] )
  ([p] [:dadysql/path p]))


(defn get-error-path
  ([] [:dadysql/path :dadysql/error-path] )
  ([v] [:dadysql/path :dadysql/error-path v]))


;; Clear error from here
(r/register-handler
  :dadysql/clear-error
  (fn [db [_ v]]
    (update-in db [:dadysql/path] dissoc :dadysql/error-path)))


;; Clear all value from here
(r/register-handler
  :dadysql/clear-path
  (fn [db [_ v]]
    (if v
      (assoc-in db [:dadysql/path v] nil)
      (assoc-in db [:dadysql/path] nil))))



;;Store all value here
(r/register-handler
  :dadysql/path
  (fn [db [p [cp v]]]
    (assoc-in db [p cp] v)))


(r/register-sub
  :dadysql/path
  (fn [db path] (do (reaction (get-in @db path)))))


(defn find-subscribe-key
  [input-request]
  (let [name (:dadysql.core/name input-request)
        group (:dadysql.core/group input-request)
        n (if (sequential? name)
            (first name)
            name)]
    (or group n)))


(defn as-dispatch
  [subscribe-key]
  (fn [[v e]]
    (if v
      (do
        (r/dispatch [:dadysql/clear-error])
        (r/dispatch [:dadysql/path [subscribe-key v]]))
      (r/dispatch [:dadysql/path [:dadysql/error-path {subscribe-key e}]]))))



(defn clear-store [& k-list]
  (if (empty? k-list)
    (r/dispatch [:dadysql/clear-path])
    (doseq [k k-list]
      (r/dispatch [:dadysql/clear-path k]))))


(defn build-request
  ([subscribe-key param-m]
   (let []
     {:params        param-m
      :handler       (as-dispatch subscribe-key)
      :error-handler (as-dispatch subscribe-key)}))
  ([param-m]
   (build-request (find-subscribe-key param-m) param-m)))
