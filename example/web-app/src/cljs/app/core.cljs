(ns app.core
  (:require-macros [reagent.ratom :refer [reaction]]
                   [secretary.core :refer [defroute]])
  (:require [goog.dom :as gdom]
            [reagent.core :as r]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]
            [devcards.util.edn-renderer :as d]
            [app.rregister ]
            [app.hiccup-view :as u]))


;(devcards.core/start-devcard-ui!)

(defn menu-action [v]
  (dispatch [:url v]))


;[:pull :name [:get-tarifasm] :params {:prodcode "FKKA"}]
;[:pull :gname :load-instance :params {:instanceid 17413191}]

(def menu [["Home" "#" menu-action]
           ["Employee" "#" menu-action]
           ])



#_(def menu [["Home" "#" [:not-found {:empty "Empty state  "}]]
           ["Department" "#" [:pull :name [:get-dept-list]]]
           ["OneEmployee" "#" [:pull :gname :load-employee
                               :params {:id 1}]]
           ["Employee" "#" [:pull :name [:get-employee-list]]]
           ["Meeting" "#" [:pull :name [:get-meeting-list]]]])




(defn content-view []
  (let [data (subscribe [:pull])]
    (fn []
      (if (empty? @data)
        [:span "Click menu to view data  "]
        (d/html-edn @data)
        ))))



(defn content-header []
  (let [url (subscribe [:url])]
    (fn []
      (cond
        (= @url "Employee")
        [u/employee-search-view]
        :else [:div]
        ))))



(defn app-content []
  [:span
   [:nav {:class "navbar navbar-default navbar-fixed-top"}
    [:div {:class "container"}
     [:div {:class "navbar-collapse collapse"}
      [:ul {:class "nav navbar-nav navbar-right"}
       [:li {:class "active"}
        [:a {:href "#"} "Hello"]]]]]]
   [:div {:class "container-fluid"}
    [:div {:class "row"}
     [:div {:class "col-sm-3 col-md-2 sidebar"}
      [u/navigation-view menu]]
     [:div {:class "col-sm-9 col-sm-offset-3 col-md-10 col-md-offset-2 main"}
      [content-header]
      [content-view]]]]])





(defn run []
  (r/render-component [app-content]
                      (gdom/getElement "content"))
  )

(run)


#_(r/render-component [init-app]
                      (gdom/getElement "app"))

