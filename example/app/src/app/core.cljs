(ns app.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [dadysql.client :as dc]))


(defn add-key-for-table-data [[headers & rows]]
  (cons headers
        (for [r rows]
          (map (fn [r1 h1]
                 [r1 (str h1 r1)]
                 ) r headers))))

(defn table [data & {:keys [on-row-click]}]
  (let [[header & row] (add-key-for-table-data data)]
    [:div.table-responsive
     [:table {:class "table table-striped table-hover"}
      [:thead
       [:tr
        (for [h header]
          [:th {:key h} (str h)])]]
      [:tbody
       (for [r row]
         [:tr {:key      r
               :on-click #(on-row-click r)}
          (for [[c k] r]
            [:td {:key k} c])])]]]))


(defn employee-list []
  (let [s (rf/subscribe (dc/sub-path :get-employee-list))]
    (fn []
      [:div
       [:p "Display emplyee list "]
       [:div "asdf" #_(pr-str @s)]])))



(defn ^:export run []
  (r/render-component [employee-list]
                      (.-body js/document)))