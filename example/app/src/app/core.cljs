(ns ^:figwheel-always app.core
  (:require [dadysql.client :as dadysql]
            [devcards.core]

    ;[sablono.core :as sab]
            [cljs.core.async :refer [<! >! timeout chan]])
  (:require-macros
    [cljs.core.async.macros :refer [go]]
    [devcards.core :as dc :refer [defcard deftest defcard-rg]]
    [cljs.test :refer [is testing async]]
    [dadysql.devcard :refer [defcard-dadysql]]))


(defn fig-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  ;        (query "http://localhost:3000/tie" [:get-dept-by-id] {:id 1} handler)
  )




#_(defcard-rg rg-example-2
              "Data View "
              [a/main-component])

;(js/alert "Hello")

#_(defcard Hello
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
               :name :get-dept-by-id
               :params {:id 1}
               :callback (fn [v]
                           (print v)

                           ))


#_(defcard my-first-card
           (sab/html [:a {:href "#/users/he"} "Clieck here !"]))

;(devcards.core/start-devcard-ui!)

(defcard-dadysql get-dept-by-id
                "**With name keyword**"
                dadysql/pull
                {:name   :get-dept-by-id
                 :params {:id 1}})



#_(defcard-dadysql employee-by-id
                  "**Join example**"
                  dadysql/pull
                  :name [:get-employee-by-id :get-employee-dept]
                  :params {:id 1})




(defcard-dadysql load-dept
                "**Load Department 2**  "
                dadysql/pull
                {:gname  :load-dept
                 :params {:id 1}})


(defcard-dadysql load-employee
                "**Load Employee**  "
                dadysql/pull
                :gname :load-employee
                :params {:id 1})




(defcard-dadysql dept-list
                "Load dept list as array  "
                dadysql/pull
                :name [:get-dept-list])



#_(defcard-dadysql insert-dept
                  "Create department  "
                  dadysql/push!
                  :name [:create-dept]
                  :params {:department {:dept_name "Call Center 9"}})






#_(defcard-dadysql create-employee
                  "Create employee  "
                  dadysql/push! "/"
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
      (<! (dadysql/pull "/"
                       :name :get-dept-by-id
                       :params {:id 1}
                       ))))


;(set! devcards.core/test-timeout 5000)













