(ns app.component.deal
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as r]
            [reagent-forms.core :refer [bind-fields init-field value-of]]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]
            [tiesql.ui :as v]
            [tiesql.re-frame :as tr]
            [app.service :as s]
            [app.component.ui :as appui]))



(def employee-template
  [:div.form-group
   [:input.form-control {:field       :numeric
                         :placeholder "Employee id "
                         :id          :search.id}]
   [:div.alert.alert-danger {:field :alert :id :errors.id}]])


(defn null-check []
  [:div {:class "checkbox"}
   [:label
    [:input {:type "checkbox"} "Null"]]])


(defn employee-search-view []
  (let [doc (r/atom {})]
    (fn []
      [:div
       [bind-fields
        employee-template
        doc]
       [:button.btn.btn-primary
        {:on-click
         #(do
           (if (get-in @doc [:search :id])
             (s/load-deal-by-id (get-in @doc [:search :id]))
             (do
               (swap! doc assoc-in [:errors :id] "Id is empty or not number "))))}
        "Search"]])))


(defn deal-list-view-template []
  (let [data (tr/subscribe [:deals])]
    (fn []
      (appui/mdl-card-batch @data)
    ;  (print "---deals " @data )
      #_[:div
       (for [i @data]
         [:div
          (appui/mdl-card i )
          ])]
      )))


#_(defn employee-content-view []
  (let [data (tr/subscribe [:load-employee])]
    (fn []
      (when @data
        (v/show-edn @data)))))


(defn deal-view-template []
  [:div
   [deal-list-view-template]
   #_[employee-search-view]
   #_[employee-content-view]])


(def deal-view
  (with-meta
    deal-view-template
    {:getInitialState #(s/load-deals)})
  )
