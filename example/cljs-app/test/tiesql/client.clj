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




(comment


  (client-test/get "http://google.com")

  (->
    (client/get "http://localhost:3000/tie/pull?name=get-dept-by-id&id=1")
    (:body)
    (clojure.pprint/pprint))


  (->> (tiesql/pull "http://localhost:3000/tie"
                    :name :get-dept-by-id
                    :params {:id 1})
       (clojure.pprint/pprint))


  (->> (tiesql/pull "http://localhost:3000/tie"
                    :name [:get-dept-by-id]
                    :params {:id 1})
       (clojure.pprint/pprint))


  (->> (tiesql/pull "http://localhost:3000/tie"
                    :name :get-dept-by-id
                    :params {:id 1}
                    :rformat :array)
       (clojure.pprint/pprint))



  #_(let [v {:department {:dept_name "Call Center 8"}}]
      (-> (ds/connection @conn)
          (tj/execute! @tms [:insert-dept] v)
          (clojure.pprint/pprint)))


  (->> {:department {:dept_name "IT"}}
       (tiesql/push! "http://localhost:3000/tie"
                     :name [:insert-dept]
                     :params)
       (clojure.pprint/pprint))

  )