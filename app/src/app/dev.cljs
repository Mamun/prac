(ns ^:figwheel-always app.dev
  (:require [app.core :as ac]
            [dadysql.client :as dc]
            [ajax.core :as a]
            [devcards.core]
            [devcards.util.edn-renderer :as d]
            [reagent.core :as r]
            [dadysql.clj.walk :as w]
            [re-frame.core :as re])
  (:require-macros
    [devcards.core :as dc :refer [defcard deftest defcard-rg]]))


(defn fig-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  ;        (query "http://localhost:3000/tie" [:get-dept-by-id] {:id 1} handler)
  )


;(re/clear-store)




#_(let [s (re/subscribe (dc/sub-path :load-employee))]
    (defcard Load-Employee
             "Date view "
             s))



(defn reagent-component-example [e-atom]
  (let [s-text (r/atom nil)]
    [:div
     [:input {:type        "text"
              :placeholder "filter value "
              :on-change (fn [event]
                           (reset! s-text (-> event .-target .-value))
                           )  #_(do
                     ;       (.log js/console (-> % .-target .-value))
                             ;(swap! f-atom assoc-in [:filter-text] )
                             (swap! s-text   (-> % .-target .-value))
                             )}]
     [:br]
     [:div
      (.log js/console (pr-str @s-text) )
      (.log js/console (pr-str (w/postwalk-filter @s-text @e-atom)) )
      (d/html-edn (if @s-text
                    (w/postwalk-filter @s-text @e-atom)
                    @e-atom

                    ))]]))




(let [s (re/subscribe (dc/sub-path :load-employee))]
  (defcard-rg my-first-card
              "Reagent view "
              [reagent-component-example s]
              s))



;(re/clear-store [])

(let [s (re/subscribe (dc/sub-path))]
  (defcard All
           "all view "
           s))





#_(let [s (re/subscribe (dc/sub-error-path))]
    (defcard Error
             "Error view "
             s))



(->> (dc/pull "/app" {:dadysql.core/group :load-employee
                      :dadysql.core/param {:id 1}}))


(comment


  (->> (dc/pull "/app" {:dadysql.core/group :load-employee
                        :dadysql.core/param {:id 1}}))

  (->> (dc/pull "/app" {:dadysql.core/name  [:get-employee-by-id :get-employee-dept :get-employee-detail]
                        :dadysql.core/param {:id 1}}))

  (dc/pull "/tie" {:dadysql.core/name [:get-employee-list]})

  (dc/pull {:dadysql.core/name  :get-dept-by-id
            :dadysql.core/param {:id 1}})


  (re/dispatch (dc/dispatch-path :get-dept-by-id {:id 3}))

  )




