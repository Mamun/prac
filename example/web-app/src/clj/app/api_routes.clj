(ns app.api-routes
  (:use compojure.core)
  (:require [tiesql.middleware :as u]
            [tiesql.http-service :as h]
            [clojure.tools.logging :as log]
            [tiesql.jdbc :s j]
            [app.state :as s]
            [tiesql.jdbc :as j]))





(defn load-deals []
  (let [v (j/pull @s/ds-atom @s/tms-atom {:name :get-deal-list})]
    (log/info "---" v)
    v))

(defroutes
  api-routes
  (GET "/deals" _ (h/http-response (load-deals) ) )

  )



(comment

  (load-deals)
  )