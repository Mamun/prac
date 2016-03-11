(ns app.core
  (:require-macros [reagent.ratom :refer [reaction]]
                   [secretary.core :refer [defroute]])
  (:require [goog.dom :as gdom]
            [devcards.util.edn-renderer :as edn]
           ; [devcards.core :as dc]
            [reagent.core :as r]
            [secretary.core :as secretary]
            [pushy.core :as pushy]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]
            [app.hiccup-ui :as u]
            [app.util :as util]))


;(devcards.core/start-devcard-ui!)


(defroute "*" [] (js/console.log "home path "))
(secretary/set-config! :prefix "/")
(def history (pushy/pushy secretary/dispatch!
                          (fn [x] (when (secretary/locate-route x) x))))

;; Start event listeners
(pushy/start! history)


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
           ["Department" "/pull?name=get-dept-list"  [:pull :name [:get-dept-list]]]
           ["OneEmployee" "/pull?name=get-dept-list" [:pull :gname :load-employee
                                                      :params {:id 1}]]
           ["Employee" "/pull?name=get-employee-list" [:pull :name [:get-employee-list]]]
           ["Meeting" "/pull?name=:get-meeting-list" [:pull :name [:get-meeting-list]]]])


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
  (r/render-component [u/menu-component (util/map-menu-action menu)]
                      (gdom/getElement "menu"))
  (r/render-component [main-component]
                      (gdom/getElement "app")))

(init-component)

