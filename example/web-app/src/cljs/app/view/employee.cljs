(ns app.view.employee
  (:require [reagent.core :as r]
            [reagent-forms.core :refer [bind-fields init-field value-of]]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]
            [devcards.util.edn-renderer :as d]
            [tiesql.client :as client]))


(defn load-employee [id]
  (client/pull
    :gname :load-employee
    :params {:id id}
    :callback (fn [v] (dispatch [:db v]))))


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
      [:div {:class "panel panel-default"}
       [:div {:class "panel-body"}
        [:div.page-header "Search employee "]
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
         "Search"]
        ; [null-check]
        ]])))


(defn employee-content-view []
  (let [data (subscribe [:db])]
    (fn []
      (when @data
        (d/html-edn @data)))))


(defn employee-view []
  [:div
   [employee-search-view]
   [employee-content-view]])

