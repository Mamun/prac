(ns tiesql.re-frame
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as r]
            [tiesql.client :as client]))


(def tiesql-path :tiesql)

(r/register-handler :error (fn [db [_ v]] (assoc-in db [:error] v)))
(r/register-sub :error (fn [db _] (reaction (get-in @db [:error]))))


(r/register-sub
  tiesql-path
  (fn
    [db path]                                               ;; db is the app-db atom
    (reaction (get-in @db path))))


(r/register-handler
  tiesql-path
  (fn [db [_ [m v]]]
    (assoc-in db [tiesql-path m] v)))


(defn pull [& {:keys [subscribe-key gname name] :as api-map}]
  (let [n (if (sequential? name) (first name) name)
        subscribe-path (or subscribe-key gname n)]
    (->> (assoc api-map :callback (fn [[v e]]
                                    (if v
                                      (r/dispatch [tiesql-path [subscribe-path v]])
                                      (r/dispatch [:error e]))))
         (seq)
         (apply concat)
         (apply client/pull))))


(defn subscribe [v]
  (let [p (into [tiesql-path] v)]
    (r/subscribe p)))
