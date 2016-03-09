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



(defn menu-component [menu]
  [:span
   (for [[v u h] menu]
     [:a {:on-click (fn [w] (h w) )
          :class    "mdl-navigation__link"
          :key v
          :href     u}
      [:i {:class "mdl-color-text--amber-grey-400 material-icons"
           :role  "presentation"} "inbox"]
      v])])
