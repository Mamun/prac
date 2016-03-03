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


;(defonce app-state (r/atom {}))


(register-handler
  :data
  (fn [db [_ v]]
    (merge db v)))




(register-sub
  :data
  (fn
    [db _]                                                  ;; db is the app-db atom
    (reaction @db)))


(defn click-handler
  [action-name]
  (condp = action-name

    "Department" (tiesql/pull :name [:get-dept-list]
                        :callback (fn [[model e]]
                                    (dispatch [:data model])))
    (dispatch [:data {:empty "Link not found "}])))



(defn main-component []
  (let [data (subscribe [:data])]
    (fn []
      (when-not (empty? @data)
        (edn/html-edn @data))
      #_(jhtml/edn->hiccup @data)
      #_(u/table @data))))


(def menu [["Home" "/" "home"]
           ["Department" "/pull?name=get-dept-list" "inbox"]])



(defn menu-component []
  [:span
   (for [[v u p] menu]
     [:a {:on-click #(click-handler v)
          :class    "mdl-navigation__link"
          :href     u}
      [:i {:class "mdl-color-text--amber-grey-400 material-icons"
           :role  "presentation"} p]
      v])])



;(print (r/render-to-static-markup (menu-component)))


(defn init-component []
  (r/render-component [menu-component]
                      (gdom/getElement "menu"))
  (r/render-component [main-component]
                      (gdom/getElement "app")))

(init-component)

;(swap! app-state update-in [:department 1 1] (fn [_] 5))
;(print @app-state)

;(print (name :keyword ))


;(js/alert "Hello")

;(print @app-state)