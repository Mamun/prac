(ns dadysql.re-frame
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as r]))


(def error-path :dadysql/error-path)


#_(defn sub-path
    ([] [:dadysql/store])
    ([p] [:dadysql/store p]))


#_(defn store-path [k v]
    [:dadysql/store [k v]])


#_(defn clear-path
    ([] [:dadysql/clear-store])
    ([k] [:dadysql/clear-store k]))


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


(defn subscribe
  [path]
  (r/subscribe (into [:dadysql/store] path)))


(defn clear-store
  [path]
  (r/dispatch (into [:dadysql/clear-store] path)))
