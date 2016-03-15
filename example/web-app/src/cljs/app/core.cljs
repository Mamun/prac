(ns app.core
  (:require-macros [reagent.ratom :refer [reaction]]
                   [secretary.core :refer [defroute]])
  (:require [goog.dom :as gdom]
            [devcards.util.edn-renderer :as edn]
            [reagent.core :as r]
            [app.rregister :as util]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]
            [app.hiccup-ui :as u]))


;(devcards.core/start-devcard-ui!)

(defn map-menu-action
  [menu-list]
  (into [] (map (fn [w]
                  (assoc w 2 (util/map-menu-dispatch w))
                  )) menu-list))


(def menu [["Home" "#" [:not-found {:empty "Empty state  "}]]
           ["Department" "#"  [:pull :name [:get-dept-list]]]
           ["OneEmployee" "#" [:pull :gname :load-employee
                                                      :params {:id 1}]]
           ["Employee" "#" [:pull :name [:get-employee-list]]]
           ["Meeting" "#" [:pull :name [:get-meeting-list]]]])





(defn search-box []
  (let [v (map-menu-action menu)]
    (fn []
      [:div {:class "mdl-cell mdl-card mdl-shadow--4dp portfolio-card"
             :style {:width "100%"}}
       ;"Hello "
       [u/navigation-view v]
       ])))


(defn show-box []
  (let [data (subscribe [:pull])]
    (fn []
      (if (empty? @data)
        [:span "Click menu to view data  "]
        (edn/html-edn @data)))))

;;Join meeting

(defn app-content []
  [:span {:style {:width "100%"}}
   [search-box]
   [show-box]])





(defn run []

  (r/render-component [app-content]
                      (gdom/getElement "content")))

(run)


#_(r/render-component [init-app]
                    (gdom/getElement "app"))

