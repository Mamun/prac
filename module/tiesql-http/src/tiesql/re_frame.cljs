(ns tiesql.re-frame
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as r]
            [tiesql.client :as client]))


(def r-store-key :_rkey_)

(def error-path  :_error_path_)

(r/register-handler :clear-error (fn [db [_ v]] (assoc-in db [r-store-key error-path] nil)))


(def clear-path :_clear_path_)
;(def tiesql-path :_tiesql_)


;(r/register-handler clear-path (fn [db [_ v]] (update-in db [r-store-key clear-path] nil)))


(r/register-handler r-store-key (fn [db [p [cp v]]]
                                   (assoc-in db [p cp] v)))

(r/register-sub r-store-key (fn [db path] (do (reaction (get-in @db path)))))


(defn subscribe
  ([] (subscribe []))
  ([v]
   (let [p (into [r-store-key] v)]
     (r/subscribe p))))


(defn dispatch [v]
  (r/dispatch [r-store-key v]))




(defn dispatch-pull [& {:keys [subscribe-key gname name] :as api-map}]
  (let [n (if (sequential? name) (first name) name)
        subscribe-path (or subscribe-key gname n)]
    (->> (assoc api-map :callback (fn [[v e]]
                                    (if v
                                      (do
                                        (dispatch [:clear-error ])
                                        (dispatch [subscribe-path v]))
                                      (r/dispatch [error-path e]))))
         (seq)
         (apply concat)
         (apply client/pull))))


(defn dispatch-push [& {:keys [subscribe-key gname name] :as api-map}]
  (let [n (if (sequential? name) (first name) name)
        subscribe-path (or subscribe-key gname n)]
    (->> (assoc api-map :callback (fn [[v e]]
                                    (if v
                                      (do
                                        (r/dispatch [:clear-error ])
                                        (dispatch [subscribe-path v]))
                                      (dispatch [error-path e]))))
         (seq)
         (apply concat)
         (apply client/push!))))



