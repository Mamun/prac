(ns app.ui)


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


