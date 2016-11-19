(ns ^:figwheel-always app.core
  (:require [devcards.core]
            [sablono.core :as sab]
            [clojure.spec :as s]

            [dadyspec.core :as c]
    #_[clojure.spec.gen :as gen]
            [clojure.spec :as s]
            [clojure.spec :as s])
  (:require-macros
    [devcards.core :as dc :refer [defcard deftest defcard-rg]]
    [dadyspec.core :refer [defsp]]
    ))


(defn fig-reload []
      ;; optionally touch your app-state to force rerendering depending on
      ;; your application
      ;; (swap! app-state update-in [:__figwheel_counter] inc)
      ;        (query "http://localhost:3000/tie" [:get-dept-by-id] {:id 1} handler)
      )


(defcard my-first-card
         (sab/html [:h1 "Devcards is freaking awesome!"]))



(defcard All
         "all view "
         (c/gen-spec :hello '{:student {:opt {:id int?}} }) )

(defsp hello {:student {:opt {:id int?}} })


(defcard Sample
         "Spec sample"
         (s/valid? :hello/student {:hello.student/id 12})
        )