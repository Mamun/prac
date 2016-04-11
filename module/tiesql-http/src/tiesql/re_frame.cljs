(ns tiesql.re-frame
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as r]
            [tiesql.client :as client]))


(def error-path  :_error_path_)
(r/register-handler error-path (fn [db [_ v]] (assoc-in db [error-path] v)))
(r/register-handler :clear-error (fn [db [_ v]] (assoc-in db [error-path] nil)))
(r/register-sub error-path (fn [db _] (reaction (get-in @db [error-path]))))

(def event-path :_event_path_)
(r/register-handler event-path (fn [db [_ v]] (assoc-in db [event-path] v)))
(r/register-sub event-path (fn [db path]  (reaction (get-in @db path))))


(def tiesql-path :_tiesql_)


(r/register-handler
  tiesql-path
  (fn [db [_ [m v]]]
    (assoc-in db [tiesql-path m] v)))


(r/register-sub
  tiesql-path
  (fn
    [db path]                                               ;; db is the app-db atom
    (reaction (get-in @db path))))


(defn subscribe [v]
  (let [p (into [tiesql-path] v)]
    (r/subscribe p)))


(defn dispatch-pull [& {:keys [subscribe-key gname name] :as api-map}]
  (let [n (if (sequential? name) (first name) name)
        subscribe-path (or subscribe-key gname n)]
    (->> (assoc api-map :callback (fn [[v e]]
                                    (if v
                                      (do
                                        (r/dispatch [:clear-error ])
                                        (r/dispatch [tiesql-path [subscribe-path v]]))
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
                                        (r/dispatch [tiesql-path [subscribe-path v]]))
                                      (r/dispatch [error-path e]))))
         (seq)
         (apply concat)
         (apply client/push!))))



