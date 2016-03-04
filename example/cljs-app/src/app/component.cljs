(ns app.component
  (:require-macros [reagent.ratom :refer [reaction]])
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


(defn tiesql-dispatch
  [[n & p]]
  (->> (conj (into [] p)
             :callback
             (fn [v]
               (dispatch [n v])))
       (apply tiesql/pull)))



(register-handler
  :pull
  (fn [db [_ [v e]]]
    (if v
      (merge db v)
      (merge db e))))



(register-handler
  :not-found
  (fn [db [_ v]]
    (merge (empty db) v)))



(register-sub
  :pull
  (fn
    [db _]                                                  ;; db is the app-db atom
    (reaction @db)))



(def menu [["Home" "/" [:not-found {:empty "Empty state  "}]]
           ["Department" "/pull?name=get-dept-list" [:pull :name [:get-dept-list]]]
           ["OneEmployee" "/pull?name=get-dept-list" [:pull :gname :load-employee
                                                      :params {:id 1}]]
           ["Employee" "/pull?name=get-employee-list" [:pull :name [:get-employee-list]]]
           ["Meeting" "/pull?name=:get-meeting-list" [:pull :name [:get-meeting-list]]]])



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



;(print "---" (process-menu menu))

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
  (r/render-component [u/menu-component (map-menu-action menu)]
                      (gdom/getElement "menu"))
  (r/render-component [main-component]
                      (gdom/getElement "app")))

(init-component)

;(swap! app-state update-in [:department 1 1] (fn [_] 5))
;(print @app-state)

;(print (name :keyword ))


;(js/alert "Hello")

;(print @app-state)