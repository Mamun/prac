(ns dadysql.re-frame
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as r]))


(def error-path :dadysql/error-path)


;; Clear all value from here
(r/register-handler
  :dadysql/clear-store
  (fn [db [_ v]]
    (if v
      (update-in db [:dadysql/store] dissoc v)
      (assoc-in db [:dadysql/store] nil))))


;;Store all value here
(r/register-handler
  :dadysql/store
  (fn [db [p [cp v]]]
    (assoc-in db [p cp] v)))


(r/register-sub
  :dadysql/store
  (fn [db path] (do (reaction (get-in @db path)))))


(defn mutate-store [s-key [v e]]
  (if v
    (do
      (r/dispatch [:dadysql/clear-store error-path])
      (r/dispatch [:dadysql/store [s-key v]]))
    (r/dispatch [:dadysql/store [:dadysql/error-path {s-key e}]])))


(defn ok-mutate [s-key v]
  (mutate-store s-key [v nil]))


(defn subscribe [path]
  (r/subscribe (into [:dadysql/store] path)))


(defn clear-store [path]
  (r/dispatch (into [:dadysql/clear-store] path)))


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
    :handler       #(mutate-store subscribe-key %)
    :error-handler #(mutate-store subscribe-key %)})
  ([param-m]
   (build-request (find-subscribe-key param-m) param-m)))