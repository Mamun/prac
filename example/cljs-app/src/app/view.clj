(ns app.view
  (require [json-html.core :as jhtml]
           [hiccup.core :as h]))


(defn table-structure [t-heading t-data]
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


(defn table-view [t-heading t-data]
  (h/html (table-structure t-heading t-data)))


(defn edn-view [data]
  (jhtml/edn->html data))


(defmacro adder [name p ]
  (-> (into [] p)
      (conj 7)
      (seq)
      ))

(adder "sfds" (+ 4))

(macroexpand-1 '(adder :hello (+ 3)))

