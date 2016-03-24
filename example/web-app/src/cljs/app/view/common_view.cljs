(ns app.view.common-view
  (:require
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









#_(def menu [["Home" "#" [:not-found {:empty "Empty state  "}]]
           ["Department" "#" [:pull :name [:get-dept-list]]]
           ["OneEmployee" "#" [:pull :gname :load-employee
                               :params {:id 1}]]
           ["Employee" "#" [:pull :name [:get-employee-list]]]
           ["Meeting" "#" [:pull :name [:get-meeting-list]]]])
