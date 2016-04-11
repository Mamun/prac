(ns app.component.employee
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as r]
            [reagent-forms.core :refer [bind-fields init-field value-of]]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]
            [app.component.common-view :as v]
            [tiesql.re-frame :as tr]))


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
             (tr/dispatch-pull :gname :load-employee
                               :params {:id (get-in @doc [:search :id])})
             (do
               (swap! doc assoc-in [:errors :id] "Id is empty or not number "))))}
        "Search"]])))


(defn employee-list-view []
  (let [data (tr/subscribe [:get-employee-list :employee])]
    (if-not @data
      (tr/dispatch-pull :name [:get-employee-list]))
    (fn []
      (if @data
        (v/table @data)))))


(defn employee-content-view []
  (let [data (tr/subscribe [:load-employee])]
    (fn []
      (when @data
        (v/html-edn @data)))))


(defn employee-view []
  [:div
   [employee-list-view]
   [employee-search-view]
   [employee-content-view]])

