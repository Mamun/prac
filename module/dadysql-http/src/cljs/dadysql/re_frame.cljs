(ns dadysql.re-frame
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as r]))


(def error-path-key :dadysql/error-path)
(def store-path-key :dadysql/store)

;; Clear all value from here
(r/register-handler
  :dadysql/clear-store
  (fn [db [_ v]]
    (if v
      (update-in db [store-path-key] dissoc v)
      (assoc-in db [store-path-key] nil))))


;;Store all value here
(r/register-handler
  store-path-key
  (fn [db [p [cp v]]]
    (assoc-in db [p cp] v)))


(r/register-sub
  store-path-key
  (fn [db path] (do (reaction (get-in @db path)))))


(defn dispatch [s-key [v e]]
  (if v
    (do
      (r/dispatch [:dadysql/clear-store error-path-key])
      (r/dispatch [store-path-key [s-key v]]))
    (r/dispatch [store-path-key [:dadysql/error-path {s-key e}]])))


(defn ok-mutate [s-key v]
  (dispatch s-key [v nil]))


(defn subscribe [path]
  (r/subscribe (into [store-path-key] path)))


(defn clear-store [path]
  (r/dispatch (into [:dadysql/clear-store] path)))


