(ns app.component
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [goog.dom :as gdom]
            [tiesql.client :as tiesql]
            [reagent.core :as r]
            [json-html.core :as jhtml]
            [app.ui :as u]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]))


;(defonce app-state (r/atom {}))


(register-handler
  :init
  (fn [db [_ v]]
    (merge db v)))




(register-sub
  :department
  (fn
    [db _]                                                  ;; db is the app-db atom
    (reaction (:department @db))))



(defn pull-dept
  []
  (tiesql/pull :name [:get-dept-list]
               :callback (fn [[model e]]
                           (dispatch [:init model]))))

;(dispatch [:init {:department []}])
;(pull-dept )

;(dispatch-sync [:init])

;(print "------" @(subscribe [:department]))

(defn simple-component []
  (let [department (subscribe [:department])]
    (fn []
      (jhtml/edn->hiccup @department)
      #_(u/table @department))))



(defn dept-list [id params]
  (pull-dept)
  (r/render-component [simple-component]
                      (gdom/getElement id)))


;(swap! app-state update-in [:department 1 1] (fn [_] 5))
;(print @app-state)

;(print (name :keyword ))


;(js/alert "Hello")

;(print @app-state)