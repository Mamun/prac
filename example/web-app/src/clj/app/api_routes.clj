(ns app.api-routes
  (:use compojure.core)
  (:require [tiesql.middleware :as u]
            [app.state :as s]))




(defroutes
  api-routes
  (GET "/deals" _  (u/ok-response {:a 7}))

  )