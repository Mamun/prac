(ns ^:figwheel-always app.dev
  (:require [app.core :as ac]
            [dadysql.client :as dc]
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




(defcard details-view
         "Deatils view "
         (r/as-element [ac/employee-data-view])
         )


;(re/clear-store [])

(let [s (re/subscribe (dc/sub-path))]
  (defcard All
           "app data  "
           s))






#_(let [s (re/subscribe (dc/sub-error-path))]
    (defcard Error
             "Error view "
             s))








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




