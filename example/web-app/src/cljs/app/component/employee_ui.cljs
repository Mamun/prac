(ns app.component.employee-ui
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as r]
            [reagent-forms.core :refer [bind-fields init-field value-of]]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]
            [devcards.util.edn-renderer :as d]
            [app.component.common-view :as v]
            [tiesql.client :as client]
            [tiesql.util :as tu]
            ))


;(def model :employee)
;(def model-list :)


(defn load-employee [id]
  (client/pull
    :gname :load-employee
    :params {:id id}
    :callback (fn [[v e]] (dispatch [:model-employee v]))))


(defn load-employee-list [callback]
  (print "load employee list ")
  (client/pull
    :name [:get-employee-list]
    :callback callback))


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
             (load-employee (get-in @doc [:search :id]))
             (do
               (swap! doc assoc-in [:errors :id] "Id is empty or not number "))))}
        "Search"]])))


(defn employee-list-view []
  (let [data (r/atom {})
        f (fn [[v e]]
            (do
              (swap! data merge v)))
        _ (load-employee-list f)]
    (fn []
      (v/table (:employee @data)))))


(defn employee-content-view []
  (let [data (subscribe [:model-employee])]
    (fn []
      (when @data
        (d/html-edn @data)))))


(defn employee-view []
  [:div
   [:div {:class "panel panel-default"}
    [:div {:class "panel-body"}
     [:div.page-header "Employee "]
     [employee-list-view]]]
   [employee-search-view]
   [employee-content-view]])

