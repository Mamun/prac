(ns app.util
  (:require [tiesql.client :as client]
            [re-frame.core :refer [dispatch
                                   dispatch-sync
                                   subscribe]]))


(defn tiesql-dispatch
  [[n & p]]
  (->> (conj (into [] p)
             :callback
             (fn [v]
               (dispatch [n v])))
       (apply client/pull)))


(defn map-menu-dispatch
  [[_ _ w]]
  (fn [_]
    (if (= (first w) :pull)
      (tiesql-dispatch w)
      (dispatch w))))



;(map-menu-dispatch ["Department" "/pull?name=get-dept-list" [:remote-pull :name [:get-dept-list]]])

(defn map-menu-action
  [menu-list]
  (into [] (map (fn [w]
                  (assoc w 2 (map-menu-dispatch w))
                  )) menu-list))
