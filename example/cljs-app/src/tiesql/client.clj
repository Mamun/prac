(ns tiesql.client
    (:require [clj-http.client :as client]))


(defn make-request
      [request-m]
      {:form-params  (select-keys request-m [:name :params :gname :pformat :rformat])
       :as           :transit+json
       :accept       :transit+json
       :content-type :transit+json})


(defn call-http-service
      [url request-m]
      (let [res (client/post url (make-request request-m))]
           (if (= (:status res)
                  200)
             (:body res)
             [nil res])))


(defn error?
      [name-coll params]
      false
      #_(cond
          (not (sequential? name-coll))
          [nil "Name will be sequential"]
          (not (map? params))
          [nil "Param will be map"]
          :else
          false))


(defn pull
      [url & {:keys [name params]
              :or   {params {}}
              :as   request-m}]
      (if-let [e (error? name params)]
              e
              (call-http-service (str url "/pull") request-m)))


(defn push!
      [url & {:keys [name params]
              :as   request-m}]
      (if-let [e (error? name params)]
              e
              (call-http-service (str url "/push") request-m)))




