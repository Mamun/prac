(ns app.ui
  (:require [sablono.core :as html :refer-macros [html]]
            [json-html.core :as jhtml]))


(defn table [[header & row]]
  (html
    [:table.mdl-data-table.mdl-js-data-table.mdl-data-table--selectable
     [:thead
      [:tr
       (for [h header]
         [:th.full-width {:key h} (str h)])]]
     [:tbody
      (for [r row]
        [:tr {:key r}
         (for [c r]
           [:td {:key c} (str c)])])]]
    ))




(defn edn [data]
  (html (jhtml/edn->hiccup data)))
