(ns app.component
  (:require-macros [reagent.ratom :refer [reaction]]
                   [tiesql.macro :refer [dispatch-tiesql-pull]])
  (:require [goog.dom :as gdom]
            [devcards.util.edn-renderer :as edn]
            [tiesql.client :as tiesql]
            [reagent.core :as r]
            [app.ui :as u]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]))


#_(defn remote-pull [handler]
  (fn [db [t & w]]
    (tiesql.client/pull
      (into [])
      :callback (fn [v]
                  (handler db v)
                  ))
    #_(handler db e)))


(defn remote-pull-handler
  [db [_ [v e]]]
  (if v
    (merge db v)
    (merge db e)))


(register-handler
  :remote-pull
  ;remote-pull
  remote-pull-handler)


(register-handler
  :not-found
  (fn [db [_ v]]
    (merge (empty db) v)))



(register-sub
  :pull
  (fn
    [db _]                                                  ;; db is the app-db atom
    (reaction @db)))


(defn menu-handler
  [v]
  (condp = v
    "Department" (dispatch-tiesql-pull [:remote-pull :name [:get-dept-list]])
    "Employee"   (dispatch-tiesql-pull [:remote-pull :name [:get-employee-list]])
    "Meeting"    (dispatch-tiesql-pull [:remote-pull :name [:get-meeting-list]])
    (dispatch [:not-found {:empty "Link not found "}])))



(def menu [["Home" "/" menu-handler]
           ["Department" "/pull?name=get-dept-list" menu-handler]
           ["Employee" "/pull?name=get-employee-list" menu-handler]
           ["Meeting" "/pull?name=:get-meeting-list" menu-handler]])



(defn main-component []
  (let [data (subscribe [:pull])]
    (fn []
      (if (empty? @data)
        [:span "Click menu to view data  "]
        (edn/html-edn @data))

      #_(jhtml/edn->hiccup @data)
      #_(u/table @data))))




;(print (r/render-to-static-markup (menu-component)))


(defn init-component []
  (r/render-component [u/menu-component menu]
                      (gdom/getElement "menu"))
  (r/render-component [main-component]
                      (gdom/getElement "app")))

(init-component)

;(swap! app-state update-in [:department 1 1] (fn [_] 5))
;(print @app-state)

;(print (name :keyword ))


;(js/alert "Hello")

;(print @app-state)