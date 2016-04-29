(ns tiesql.re-frame
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as r]
            [tiesql.client :as client]))


(def r-store-key :_rkey_)
(def error-path :_error_path_)
(def clear-r-store-key :_clear_rkey_)


;; Clear error from here
(r/register-handler
  :clear-error
  (fn [db [_ v]]
    (assoc-in db [r-store-key error-path] nil)))



;; Clear all value from here
(r/register-handler
  clear-r-store-key
  (fn [db [_ v]]
    (assoc-in db [r-store-key ] nil)))


;;Store all value here
(r/register-handler
  r-store-key
  (fn [db [p [cp v]]]
    (assoc-in db [p cp] v)))


(r/register-sub
  r-store-key
  (fn [db path] (do (reaction (get-in @db path)))))


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
                                        (r/dispatch [:clear-error])
                                        (dispatch [subscribe-path v]))
                                      (dispatch [error-path e]))))

         (client/pull))))


(defn dispatch-push [& {:keys [subscribe-key gname name] :as api-map}]
  (let [n (if (sequential? name) (first name) name)
        subscribe-path (or subscribe-key gname n)]
    (->> (assoc api-map :callback (fn [[v e]]
                                    (if v
                                      (do
                                        (r/dispatch [:clear-error])
                                        (dispatch [subscribe-path v]))
                                      (dispatch [error-path e]))))
         (client/push!))))



