(ns app.component.ui)

;"images/donner.jpg"

(defn mdl-card [deal ]
  [:div {:class "mdl-grid mdl-cell mdl-shadow--4dp mdl-cell--12-col-desktop mdl-cell--6-col-tablet mdl-cell--4-col-phone "}
   [:div {:class "mdl-cell "}
    [:img {:class " article-image"
           :src   (get deal :image_link)}]
    #_[:button {:class "mdl-button mdl-js-button mdl-button--raised mdl-button--accent"}
       "View Deals"]
    [:span
     [:span {:class "original_price"}
      (get deal :orginal_price)]
     #_[:button {:class "mdl-button mdl-button--accent"}
        (get deal :deal_price)]
     [:span "  "]
     [:span {:class "discount_price"}
      (get deal :deal_price)]]]
   [:div {:class "mdl-cell mdl-cell--8-col"}
    [:h2 {:class "mdl-card__title-text"} (get deal :title)]
    [:div {:class "mdl-card__supporting-text padding-top"}
     [:p (get deal :description)]]
    [:div {:class "mdl-card__supporting-text"}
     [:p (get deal :d)]]]])


(defn mdl-card-batch [deal-list]
  [:div
   (for [v deal-list]
     (mdl-card v))])
