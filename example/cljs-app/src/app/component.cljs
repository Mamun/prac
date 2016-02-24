(ns app.component
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [sablono.core :as html :refer-macros [html]]))


(defn table-view [t-heading t-data]
  [:div.panel.panel-default
   [:div.panel-heading (str t-heading)]
   [:div.panel-body
    [:div.table-responsive
     [:table.table.table-bordered
      [:thead
       (for [h (first t-data)]
         [:th (str h)])]
      [:tbody
       (for [r (rest t-data)]
         [:tr
          (doall (for [c r]
                   [:td (str c)]))])]]]]])


(defui Counter
       Object
       (render [this]
               (let [data (om/props this)]
                 (html (table-view "Hello " data)))))


(defn show-counter
  [state]
  (-> (om/reconciler {:state state })
      (om/add-root! Counter (gdom/getElement "app"))))


;(def reconsi (om/reconciler {:state app-state}))


;(om/add-root! reconsi Counter (gdom/getElement "app"))