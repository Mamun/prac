(ns app.service
  (:require [tiesql.re-frame :as tr]
            [ajax.core :as a]
            [tiesql.client :as client]))


(defn load-deal-by-id [id]
  (->> (tr/build-ajax-request {:gname :load-employee :params {:id id}})
       (client/pull)))



(defn load-deals []
  (->> (tr/build-ajax-request :deals {})
       (client/default-ajax-params)
       (a/GET "/api/deals" )))

;(load-deals)