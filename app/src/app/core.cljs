(ns app.core
  (:require [reagent.core :as r]
            [devcards.util.edn-renderer :as d]
            [re-frame.core :as rf]
            [dadysql.client :as dc]
            [dadysql.clj.walk :as w]
            [re-frame.core :as re]))



(defn load-employee [id]
  (dc/pull "/app" {:dadysql.core/group :load-employee
                   :dadysql.core/param {:id id}}))


(defn employee-data-view []
  (let [e-atom (re/subscribe (dc/sub-path :load-employee))
        error-atom (re/subscribe (dc/sub-error-path :load-employee))
        local-state (r/atom {:id nil :filter-text nil})]
    (fn []
      [:div
       [:input {:type        "text"
                :placeholder "Load employee "
                :on-change   (fn [event] (swap! local-state assoc-in [:id] (-> event .-target .-value)))}]
       (if @error-atom
         [:dev
          [:br]
          [:text @error-atom]
          [:br]])
       [:button {:on-click (fn [_] (load-employee (get @local-state :id)))}
        "Load employee "]
       [:br]
       [:input {:type        "text"
                :placeholder "filter value "
                :on-change   (fn [event] (swap! local-state assoc-in [:filter-text] (-> event .-target .-value)))}]
       [:br]
       [:div
        (d/html-edn (if-let [w (get @local-state :filter-text)]
                      (w/postwalk-filter w @e-atom)
                      @e-atom
                      ))]])))




(defn ^:export run []
  (r/render-component [employee-data-view]
                      (.-body js/document)))