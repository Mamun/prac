(ns ^:figwheel-always app.core
  (:require [dadysql.client :as dadysql]
            [devcards.core]
            [ajax.core :as a]
            ;[sablono.core :as sab]
            [reagent.core :as reagent]
            [dadysql.re-frame :as re]
         ;   [re-frame.core :as r]
            [cljs.core.async :refer [<! >! timeout chan]])
  (:require-macros
    [cljs.core.async.macros :refer [go]]
    [devcards.core :as dc :refer [defcard deftest defcard-rg]]
    [cljs.test :refer [is testing async]]))


(defn fig-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  ;        (query "http://localhost:3000/tie" [:get-dept-by-id] {:id 1} handler)
  )


;(re/clear-store)

(defn reagent-component-example []
  (let [s (re/subscribe [:get-employee-list])]
    (fn []
      [:div (pr-str @s)]
      )))




(defcard my-first-card
         (reagent/as-element [reagent-component-example]))

(let [s (re/subscribe [] )]
  (defcard All
           "all view "
           s))



(let [s (re/subscribe [re/error-path])]
  (defcard Error
           "Error view "
           s))



(let [s (re/subscribe [:get-employee-list :employee])]
  (defcard Hello
           "Date view "
           s))


;(re/clear-store [])

;(re/build-request {:dadysql.core/name :get-employee-list})


;(js/alert "Hello")

;(r/dispatch (re/store-path re/error-path {:error "Error"}))
;(r/dispatch (re/dispatch-path [:Check  {:error "Error"}]))

(->> (re/build-request {:dadysql.core/name [:get-employee-list]})
     (dadysql/pull))




(->> (re/build-request {:dadysql.core/name  [:get-employee-by-id :get-employee-dept :get-employee-detail]
                        :dadysql.core/param {:id 1}})
     (dadysql/pull))

;(r/dispatch (re/clear-path re/error-path) )

;(r/dispatch (re/clear-path :get-employee-by-id ) )

;(r/dispatch (re/clear-path  ) )

;(re/clear-store re/error-path)

(->> (re/build-request :get-request {:a 3})
       (a/GET "/api"))

#_(->> (re/build-request :ajax4 {:a 3})
       (a/POST "/api/hello"))

;(a/GET "")

#_(-> {:dadysql.core/name :get-employee-list}
      (dadysql/pull (re/as-dispatch :get-employee-list)))

;(a/GET "/api" (dadysql/build-request {:a 10}) )


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



#_(dadysql/pull "/"
                :dadysql.core/name :get-dept-by-id
                :params {:id 1}
                :callback (fn [v]
                            (print v)

                            ))


#_(defcard my-first-card
           (sab/html [:a {:href "#/users/he"} "Clieck here !"]))

;(devcards.core/start-devcard-ui!)

#_(defcard-dadysql get-dept-by-id
                   "**With name keyword**"
                   dadysql/pull
                   {:dadysql.core/name   :get-dept-by-id
                    :dadysql.core/params {:id 1}})


#_(defcard-dadysql employee-by-id
                   "**Join example**"
                   dadysql/pull
                   :dadysql.core/name [:get-employee-by-id :get-employee-dept]
                   :params {:id 1})


#_(defcard-dadysql load-dept
                   "**Load Department 2**  "
                   dadysql/pull
                   {:gname  :load-dept
                    :params {:id 1}})


#_(defcard-dadysql load-employee
                   "**Load Employee**  "
                   dadysql/pull
                   {:gname  :load-employee
                    :params {:id 1}})


#_(defcard-dadysql dept-list
                   "Load dept list as array  "
                   dadysql/pull
                   {:dadysql.core/name [:get-dept-list]})


#_(defcard-dadysql insert-dept
                   "Create department  "
                   dadysql/push!
                   :dadysql.core/name [:create-dept]
                   :params {:department {:dept_name "Call Center 9"}})


#_(defcard-dadysql create-employee
                   "Create employee  "
                   dadysql/push! "/"
                   :dadysql.core/name [:create-employee :create-employee-detail]
                   :params {:employee {:firstname       "Schwan"
                                       :lastname        "Ragg"
                                       :dept_id         1
                                       :employee-detail {:street  "Schwan",
                                                         :city    "Munich",
                                                         :state   "Bayern",
                                                         :country "Germany"}}})



#_(go
    (print
      (<! (dadysql/pull "/"
                        :dadysql.core/name :get-dept-by-id
                        :params {:id 1}
                        ))))


;(set! devcards.core/test-timeout 5000)













