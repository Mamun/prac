(ns app.view.employee
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
            [tiesql.util :as tu]
            [app.view.common-view :as v]
            [tiesql.client :as client]))


;(def model :employee)
;(def model-list :)


(register-sub
  :employee
  (fn [db _]
    (reaction (-> (get-in @db [:tiesql-db])
                  (select-keys [:employee])
                  (tu/postwalk-remove-nils)
                  (tu/postwalk-replace-tag-value)))))


(register-sub
  :employee-list
  (fn [db _]
    (reaction (-> (get-in @db [:app :employee :list])

                  ;    (tu/postwalk-remove-nils)
                  (tu/postwalk-replace-tag-value)))
    ))


(register-handler
  :employee-list
  (fn [db [_ [v e]]]
    (update-in db [:app :employee :list] (fn [_] v))
    ))


;(dispatch [:employee-list])

(defn get-employee-by-id [id]
  (client/pull
    :gname :load-employee
    :params {:id id}
    :callback (fn [v] (dispatch [:tiesql-db v]))))


(defn get-employee-list []
  (client/pull
    :name [:get-employee-list]
    ;:params {:id id}
    :callback (fn [v] (dispatch [:employee-list v]))))


(get-employee-list)

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
             (get-employee-by-id (get-in @doc [:search :id]))
             (do
               (swap! doc assoc-in [:errors :id] "Id is empty or not number "))))}
        "Search"]]
      )))


(defn employee-list-view []
  (let [data (subscribe [:employee-list])]
    (fn []
      ; (print @data)
      (v/table (:employee  @data) )
      )))

(defn employee-content-view []
  (let [data (subscribe [:employee])]
    (fn []
      (when @data
        (d/html-edn @data)))))


(defn employee-view []
  [:div
   [:div {:class "panel panel-default"}
    [:div {:class "panel-body"}
     [:div.page-header "Employee "]
     [employee-list-view]



     ; [null-check]
     ]]
   [employee-search-view]
   [employee-content-view]])

