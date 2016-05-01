(ns app.component.employee
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as r]
            [reagent.dom :as d]
            [reagent-forms.core :refer [bind-fields init-field value-of]]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]
            [tiesql.ui :as v]
            [tiesql.util :as u]
            [tiesql.re-frame :as tr]
            [tiesql.client :as client]))



(defn load-employee-by-id [id]
  
  (-> {:gname :load-employee :params {:id id}}
      (client/pull (tr/as-dispatch :load-employee))))


(defn load-employee-list []
  (-> {:name :get-employee-list}
      (client/pull (tr/as-dispatch :get-employee-list))))


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
             (load-employee-by-id (get-in @doc [:search :id]))
             (do
               (swap! doc assoc-in [:errors :id] "Id is empty or not number "))))}
        "Search"]])))


(defn employee-list-view []
  (let [data (tr/subscribe [:get-employee-list])]
    (fn []

      (if @data
        (v/table @data :on-row-click (fn [v] (load-employee-by-id (first (first v)))))))))


(defn employee-content-view []
  (let [data (tr/subscribe [:load-employee])]
    (fn []
      (when @data
        (v/show-edn @data)))))


(defn employee-view2 []
  [:div
   [employee-list-view]
   [employee-search-view]
   [employee-content-view]])


(def employee-view
  (with-meta
    employee-view2
    {:getInitialState #(load-employee-list)})
  )
