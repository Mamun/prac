(ns ^:figwheel-always app.core
  (:require [dadysql.client :as dc]
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


(let [s (r/subscribe (dc/sub-path))]
  (defcard All
           "all view "
           s))


(let [s (r/subscribe (dc/sub-error-path))]
  (defcard Error
           "Error view "
           s))



(let [s (r/subscribe (dc/sub-path :get-employee-list :employee))]
  (defcard Hello
           "Date view "
           s))


(defn reagent-component-example []
  (let [s (r/subscribe (dc/sub-path :get-employee-list))]
    (fn []
      [:div (pr-str @s)])))


(defcard my-first-card
  (reagent/as-element [reagent-component-example]))



;(re/clear-store [])

(dc/pull {:dadysql.core/name [:get-employee-list]})



(comment


  (dc/pull {:dadysql.core/name  :get-dept-by-id
            :dadysql.core/param {:id 1}})


  (->> (dc/pull {:dadysql.core/name  [:get-employee-by-id :get-employee-dept :get-employee-detail]
                 :dadysql.core/param {:id 1}}))


  (r/dispatch (dc/dispatch-path :get-dept-by-id {:id 3}))

  )


;(js/alert "Hello")

(defcard Hello
         "Hello"
         {:a 3})




;(set! devcards.core/test-timeout 5000)







#_(->> (re/build-request :get-request {:a 3})
       (a/GET "/api"))

#_(->> (re/build-request :ajax4 {:a 3})
       (a/POST "/api/hello"))






