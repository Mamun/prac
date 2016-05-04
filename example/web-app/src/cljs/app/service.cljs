(ns app.service
  (:require [tiesql.re-frame :as tr]
            [ajax.core :as a]
            [tiesql.client :as client]))


(defn load-deal-by-id [id]
  (->> (tr/build-ajax-request {:gname :load-employee :params {:id id}})
       (client/pull)))


(defn load-deal-list []
  (->> (tr/build-ajax-request {:name :get-employee-list})
       (client/pull)))


(defn load-deals []
  (a/GET "/api/deals" (tr/build-ajax-request :deals {}) ))

;(load-deals)