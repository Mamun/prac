(ns dadysql.ui
  (:require
   ; [dadysql.clj.walk :as w]
    [dadysql.util :as tu]
    #_[devcards.util.edn-renderer :as d]))


#_(defn show-edn [edn-data]
  (-> (w/postwalk-replace-value-with tu/as-str edn-data)
      (d/html-edn)))


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
            [:td {:key k} (tu/as-str c)])])]]]))



(defn nav [href-list & {:keys [on-click]}]
  [:ui {:class "nav nav-sidebar"}
   (for [[v u :as l] href-list]
     [:li {:key v}
      [:a {:on-click (fn [_] (on-click l))
           :key      v
           :href     u}
       v]])])

