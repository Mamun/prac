(ns app.component
  (:require [goog.dom :as gdom]
            [tiesql.client :as tiesql]
            [om.next :as om :refer-macros [defui]]
            [app.ui :as u]))


(defonce app-state (atom {}))
(defonce reconciler (om/reconciler {:state app-state}))


(defn pull-dept
  [params]
  (tiesql/pull :name [:get-dept-list]
               :callback (fn [[model e]]
                           (swap! app-state merge model))))


(defui Department
  Object
  (render [this]
    (let [{:keys [department]} (om/props this)]
      (u/table department))
    ;(u/edn  (om/props this))
    ))



(defn dept-list [id params]
  (do
    (pull-dept params)
    (om/add-root! reconciler Department (gdom/getElement id ))))


;(swap! app-state update-in [:department 1 1] (fn [_] 5))
;(print @app-state)

;(print (name :keyword ))


