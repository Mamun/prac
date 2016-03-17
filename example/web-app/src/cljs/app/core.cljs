(ns app.core
  (:require-macros [reagent.ratom :refer [reaction]]
                   [secretary.core :refer [defroute]])
  (:require [goog.dom :as gdom]
            [devcards.util.edn-renderer :as edn]
            [reagent.core :as r]
            [app.rregister :as util]
            [reagent-forms.core :refer [bind-fields init-field value-of]]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]
            [app.hiccup-ui :as u]))


;(devcards.core/start-devcard-ui!)

(defn map-menu-action
  [menu-list]
  (into [] (map (fn [w]
                  (assoc w 2 (util/map-menu-dispatch w))
                  )) menu-list))


(def menu [["Home" "#" [:not-found {:empty "Empty state  "}]]
           ["Department" "#"  [:pull :name [:get-dept-list]]]
           ["OneEmployee" "#" [:pull :gname :load-employee
                                                      :params {:id 1}]]
           ["Employee" "#" [:pull :name [:get-employee-list]]]
           ["Meeting" "#" [:pull :name [:get-meeting-list]]]])



(defn menu-view []
  (let [v (map-menu-action menu)]
    (fn []
      [u/navigation-view v]
      )))


(defn content-view []
  (let [data (subscribe [:pull])]
    (fn []
      (if (empty? @data)
        [:span "Click menu to view data  "]
        (edn/html-edn @data)))))

;;Join meeting

(defn row [label input]
  [:div.row
   [:div.col-md-2 [:label label]]
   [:div.col-md-5 input]])

(defn radio [label name value]
  [:div.radio
   [:label
    [:input {:field :radio :name name :value value}]
    label]])


(defn input [label type id]
  (row label [:input.form-control {:field type :id id}]))


(def form-template
  [:span
   (input "last name" :text :search.value)
   [:div.form-group
    [:input.form-control {:type :text
                          ; :field :text
                          :id   :search.value}]]])



(defn search-box []
  (let [doc  (atom {:search {:value "Hello"}})]
    (fn []
      [:div
       [:div.page-header "Search "]
       [bind-fields
        form-template
        doc]
       [:button.btn.btn-primary
        {:on-click
         #(if-not (empty? (get-in @doc [:search :value]))
           (js/console.log (get-in @doc [:search :value]))
           #_(swap! doc assoc-in [:errors :first-name] "first name is empty"))}
        "save"]
       ])
    ))


(defn app-content []
  [:span

   [:nav {:class  "navbar navbar-default navbar-fixed-top"}
    [:div {:class "container"}
     [:div {:class "navbar-collapse collapse" }
      [:ul {:class "nav navbar-nav navbar-right"}
       [:li {:class "active"}
        [:a {:href "#"} "Hello"] ]
       ]
      ]]]
   [:div {:class "container-fluid"}
    [:div {:class "row"}
     [:div {:class "col-sm-3 col-md-2 sidebar"}
      [menu-view]]
     [:div {:class "col-sm-9 col-sm-offset-3 col-md-10 col-md-offset-2 main"}
      [:div {:class "panel panel-default"}
       [:div {:class "panel-body"}
        [search-box]]]
      [content-view]]]]])





(defn run []

  (r/render-component [app-content]
                      (gdom/getElement "content")))

(run)


#_(r/render-component [init-app]
                    (gdom/getElement "app"))

