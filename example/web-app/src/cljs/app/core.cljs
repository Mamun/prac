(ns app.core
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [goog.dom :as gdom]
            [reagent.core :as r]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]
            [app.component.employee :as e]
            [tiesql.ui :as u]
            [tiesql.re-frame :as tr]))




;(register-handler :url (fn [db [_ v]] (assoc-in db [:url] v)))
;(register-sub :url (fn [db _] (reaction (get-in @db [:url]))))


;(devcards.core/start-devcard-ui!)

(defn menu-action [v] (tr/dispatch [:url (first v)]) #_(dispatch [:url v]))


(def menu [["Home" "#" ]
           ["Employee" "#" ]])



(defn error-view []
  (let [data (tr/subscribe [:error])]
    (fn []
      (when @data
        [:div.alert.alert-danger @data]))))


(defn app-content []
  (let [url (tr/subscribe [:url])]
    (fn []
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
          [u/nav menu :on-click menu-action]]
         [:div {:class "col-sm-9 col-sm-offset-3 col-md-10 col-md-offset-2 main"}
          [error-view]
          (cond
            (= @url "Employee")
            [e/employee-view]
            :else [:div "Default view "])]]]])))





(defn run []
  (r/render-component [app-content]
                      (gdom/getElement "content")))

(run)


#_(r/render-component [init-app]
                      (gdom/getElement "app"))

