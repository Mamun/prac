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
            [app.view.employee :as e]
            [app.view.common-view :as u]))



(register-handler
  :tiesql-db
  (fn [db [_ [v e]]]
    (if v
      (update-in db [:tiesql-db] merge v #_(fn [_] v))
      (update-in db [:tiesql-db] merge e #_(fn [_] e)))))



(register-sub
  :tiesql-db
  (fn
    [db _]                                                  ;; db is the app-db atom
    (reaction (get-in @db [:tiesql-db]))))




(register-handler :url (fn [db [_ v]] (assoc-in db [:url] v)))
(register-sub :url (fn [db _]  (reaction (get-in @db [:url]))))


;(devcards.core/start-devcard-ui!)

(defn menu-action [v] (dispatch [:url v]))


(def menu [["Home" "#" menu-action]
           ["Employee" "#" menu-action]])




(defn app-content []
  (let [url (subscribe [:url])]
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
          [u/navigation-view menu]]
         [:div {:class "col-sm-9 col-sm-offset-3 col-md-10 col-md-offset-2 main"}
          (cond
            (= @url "Employee")
            [e/employee-view]
            :else [e/employee-view])]]]])))





(defn run []
  (r/render-component [app-content]
                      (gdom/getElement "content")))

(run)


#_(r/render-component [init-app]
                      (gdom/getElement "app"))

