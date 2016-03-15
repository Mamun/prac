(ns app.hiccup-ui)


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
  [:nav {:class "mdl-list"}
   (for [[v u h] href-list]
     [:li {:class "mdl-list__item"
           :key v}
      [:a {:on-click (fn [w] (do
                            ;   (.preventDefault w)
                               (h w)))
           :class    "mdl-navigation__link"
           :key      v
           :href     u}
       #_[:i {:class "mdl-color-text--amber-grey-400 material-icons"
            :role  "presentation"} "inbox"]
       v]])
   #_[:div {:class "mdl-layout-spacer"}]]
  #_[:div {:class "mdl-layout__drawer"}
   ])



