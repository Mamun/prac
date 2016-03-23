(ns app.hiccup-view
  (:require [reagent-forms.core :refer [bind-fields init-field value-of]]
            [reagent.core :as r]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]))


(defn table [[header & row]]
  [:div {:class "mdl-shadow--2dp mdl-color--white mdl-cell"}
   [:table {:class "mdl-data-table mdl-js-data-table mdl-data-table--selectable"}
    [:thead
     [:tr
      (for [h header]
        [:th {:key h} (str h)])]]
    [:tbody
     (for [r row]
       [:tr {:key r}
        (for [c r]
          [:td {:key c} (str c)])])]]])



(defn navigation-view [href-list]
  [:ui {:class "nav nav-sidebar"}
   (for [[v u h] href-list]
     [:li {:key v}
      [:a {:on-click (fn [_] (h v))
           ;:class    "mdl-navigation__link"
           :key      v
           :href     u}
       v]])])



(defn null-check []
  [:div {:class "checkbox" }
   [:label
    [:input {:type "checkbox" } "Null"]]] )



(def employee-template
  [:div.form-group
   [:input.form-control {:field :numeric
                         :placeholder "Employee id "
                         :id :search.id}]
   [:div.alert.alert-danger {:field :alert :id :errors.id}]])



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
            (print @doc)
            (if (get-in @doc [:search :id])
              (dispatch [:pull
                         [:gname :load-employee
                          :params {:id (get-in @doc [:search :id])}]])
              (do
                (swap! doc assoc-in [:errors :id] "Id is empty or not number "))))}
         "Search"]
        ; [null-check]
        ]])))



#_(def menu [["Home" "#" [:not-found {:empty "Empty state  "}]]
           ["Department" "#" [:pull :name [:get-dept-list]]]
           ["OneEmployee" "#" [:pull :gname :load-employee
                               :params {:id 1}]]
           ["Employee" "#" [:pull :name [:get-employee-list]]]
           ["Meeting" "#" [:pull :name [:get-meeting-list]]]])
