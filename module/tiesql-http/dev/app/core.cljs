(ns ^:figwheel-always app.core
  (:require [tiesql.client :as tiesql]
            [devcards.core]
            [ajax.core :as a]
    ;[sablono.core :as sab]
            [cognitect.transit :as t]
            [tiesql.re-frame :as re]
            [cljs.core.async :refer [<! >! timeout chan]])
  (:require-macros
    [cljs.core.async.macros :refer [go]]
    [devcards.core :as dc :refer [defcard deftest defcard-rg]]
    [cljs.test :refer [is testing async]]
    [tiesql.devcard :refer [defcard-tiesql]]
    ))


(defn fig-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  ;        (query "http://localhost:3000/tie" [:get-dept-by-id] {:id 1} handler)
  )


;(re/clear-store)


(let [s (re/subscribe [])]
  (defcard All
           "all view "
           s))



(let [s (re/subscribe [re/error-path :get-employee-list])]
  (defcard Error
           "Error view "
           s))



(let [s (re/subscribe [:get-employee-list])]
  (defcard Hello
           "Date view "
           s))

;(re/build-request {:name :get-employee-list})




(->> (re/build-ajax-request {:name :get-employee-list})
     (tiesql/pull))


;(a/GET "")

#_(-> {:name :get-employee-list}
      (tiesql/pull (re/as-dispatch :get-employee-list)))

;(a/GET "/api" (tiesql/build-request {:a 10}) )


#_(defcard-rg rg-example-2
              "Data View "
              [a/main-component])

;(js/alert "Hello")

(defcard Hello
         "Hello"
         {:a 3})


#_(defcard Hello
           "Hello from dev card "
           ;(failed? (fail "Hello"))
           (v/failed?
             ;{:1 2}
             (v/fail "Hello")))


#_(deftest Checktest
           (testing "sfsdf"
             (is (= 1 1))))



#_(tiesql/pull "/"
               :name :get-dept-by-id
               :params {:id 1}
               :callback (fn [v]
                           (print v)

                           ))


#_(defcard my-first-card
           (sab/html [:a {:href "#/users/he"} "Clieck here !"]))

;(devcards.core/start-devcard-ui!)

#_(defcard-tiesql get-dept-by-id
                  "**With name keyword**"
                  tiesql/pull
                  {:name   :get-dept-by-id
                   :params {:id 1}})


#_(defcard-tiesql employee-by-id
                  "**Join example**"
                  tiesql/pull
                  :name [:get-employee-by-id :get-employee-dept]
                  :params {:id 1})


#_(defcard-tiesql load-dept
                  "**Load Department 2**  "
                  tiesql/pull
                  {:gname  :load-dept
                   :params {:id 1}})


#_(defcard-tiesql load-employee
                  "**Load Employee**  "
                  tiesql/pull
                  {:gname  :load-employee
                   :params {:id 1}})


#_(defcard-tiesql dept-list
                  "Load dept list as array  "
                  tiesql/pull
                  {:name [:get-dept-list]})


#_(defcard-tiesql insert-dept
                  "Create department  "
                  tiesql/push!
                  :name [:create-dept]
                  :params {:department {:dept_name "Call Center 9"}})


#_(defcard-tiesql create-employee
                  "Create employee  "
                  tiesql/push! "/"
                  :name [:create-employee :create-employee-detail]
                  :params {:employee {:firstname       "Schwan"
                                      :lastname        "Ragg"
                                      :dept_id         1
                                      :employee-detail {:street  "Schwan",
                                                        :city    "Munich",
                                                        :state   "Bayern",
                                                        :country "Germany"}}})



#_(go
    (print
      (<! (tiesql/pull "/"
                       :name :get-dept-by-id
                       :params {:id 1}
                       ))))


;(set! devcards.core/test-timeout 5000)













