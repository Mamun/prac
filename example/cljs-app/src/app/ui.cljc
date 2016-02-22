(ns app.ui)


(defn table-view [t-heading t-data]
  [:div.panel.panel-default
   [:div.panel-heading (str t-heading)]
   [:div.panel-body
    [:div.table-responsive
     [:table.table.table-bordered
      [:thead
       (doall (for [h (first t-data)]
                [:th (str h)])
              )]
      [:tbody
       (doall (for [r (rest t-data)]
                [:tr
                 (doall (for [c r]
                          [:td (str c)]))]))]]]]])
