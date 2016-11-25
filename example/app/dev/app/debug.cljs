(ns ^:figwheel-always app.debug
  (:require [app.core :as ac]
            [dadysql.client :as dc]
            [ajax.core :as a]
            [devcards.core]
            [reagent.core :as reagent]
            [re-frame.core :as r])
  (:require-macros
    [devcards.core :as dc :refer [defcard deftest defcard-rg]]))


(defn fig-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  ;        (query "http://localhost:3000/tie" [:get-dept-by-id] {:id 1} handler)
  )


;(re/clear-store)




(let [s (r/subscribe (dc/sub-path :get-employee-list :employee))]
  (defcard Hello
           "Date view "
           s))


(defn reagent-component-example []
  (let [s (r/subscribe (dc/sub-path :get-employee-list))]
    (fn []
      [:div (pr-str @s)])))



(defcard employee-list
    (reagent/as-element [reagent-component-example]))



;(re/clear-store [])

(let [s (r/subscribe (dc/sub-path))]
  (defcard All
           "all view "
           s))


(let [s (r/subscribe (dc/sub-error-path))]
  (defcard Error
           "Error view "
           s))






(comment


  (->> (dc/pull "/tie" {:dadysql.core/group  :load-employee
                        :dadysql.core/param {:id 1}}))

  (->> (dc/pull "/tie" {:dadysql.core/name  [:get-employee-by-id :get-employee-dept :get-employee-detail]
                        :dadysql.core/param {:id 1}}))

  (dc/pull "/tie" {:dadysql.core/name [:get-employee-list]})

  (dc/pull {:dadysql.core/name  :get-dept-by-id
            :dadysql.core/param {:id 1}})


  (r/dispatch (dc/dispatch-path :get-dept-by-id {:id 3}))

  )




