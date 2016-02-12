(ns app.client-test
    #_(:require [app.client :as tiesql]
      ;    [clj-http.client :as client]

              ))



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