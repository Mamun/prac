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
            [tiesql.client :as client]
            [tiesql.util :as tu]))


(def model-employee :model-employee)


(register-handler
  model-employee
  (fn [db [_  v]]
    (update-in db [model-employee] (fn [_] v ))))


(register-sub
  model-employee
  (fn [db _]
    (reaction (-> (get-in @db [model-employee])
                  (select-keys [:employee])
                  (tu/postwalk-remove-nils)
                  (tu/postwalk-replace-tag-value)))))


(defn load-employee [id]
  (client/pull
    :gname :load-employee
    :params {:id id}
    :callback (fn [[v e]]
                (if v
                  (dispatch [model-employee v])
                  (dispatch [:error] e)))))


(defn load-employee-list [callback]
  ;(print "load employee list ")
  (client/pull
    :name [:get-employee-list]
    :callback (fn [[v e]]
                (if v
                  (callback (:employee v))
                  (dispatch [:error] e)))))


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
  (let [data (r/atom nil)
        f (fn [v]
            (do
              (reset! data v )))
        _ (load-employee-list f)]
    (fn []
      (if @data
        (v/table @data)))))


(defn employee-content-view []
  (let [data (subscribe [model-employee])]
    (fn []
      (when @data
        (v/html-edn @data)))))


(defn employee-view []
  [:div
   [employee-list-view]
   [employee-search-view]
   [employee-content-view]])

