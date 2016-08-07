(ns rscrap.rscraper
  (require                                                  ;[pl.danieljanus.tagsoup :as ts]
    [environ.core :as e]
    [net.cgrand.enlive-html :as html]
    [clj-http.client :as client]
    [rscrap.adapter :as a]
    [clojure.tools.reader.edn :as edn]
    [net.cgrand.tagsoup]))


(defn log [v]
  (println "-----")
  (clojure.pprint/pprint v)
  (println "Log done-------------- "))


#_(defn build-credit-request [m]
  (let [req {"CAM_Instance_theDossierConditions_mCreditTypeCode"      0
             "Instance_theDossierConditions_mPaymentDay"              1
             "Instance_theDossierConditions_theMaterialInfo$0_mTaken" "E"
             "Instance_theDossierConditions_theMaterialInfo$0_mPrice" 2000
             "CAM_mCreditAmount"                                      2000}]
    (merge-with merge m {:params req}))
  )



(defn as-string [n]
  (reduce str (html/emit* n)))


(defn as-file [n]
  (spit "data.html" (as-string n)))






(comment


  ;;Submit material
  (let [user-params (edn/read-string (slurp "params.edn"))]

    (binding [a/*base-url* "https://green-1.commerzfinanz.com"]

      (a/submit-page (a/as-request a/login-url a/login-data))

      (->
        (a/get-page (a/as-request a/material-url {}))
        (a/assoc-user-params user-params)
        (a/submit-page)

        (a/assoc-user-params user-params)
        (current-page)
        (a/submit-page)

        (a/assoc-user-params user-params)
        (a/submit-page)

        ;;custoemr identity
        (a/assoc-user-params user-params)
        (a/submit-page)

        ;;custoemr identity comple
        (a/assoc-user-params user-params)
        (a/submit-page)

        (:response)
        (as-file)
        #_(dissoc :response))

      ))


  ;;Submit credit type
  (do
    (->
      (a/get-page :credit)
      (eu/node->map))
    (-> {"Instance_theDossierConditions_theMaterialInfo$0_mPrice" 2000
         "CAM_mCreditAmount"                                      2000}
        (assoc "RequestID" "-80942774")
        (assoc "UNIQUE_TRANSACTION" "FLOWTID:2")
        (a/submit-page :credit)
        (eu/node->map)
        ;(dissoc "RequestID")
        #_(build-credit-request)
        ;   (assoc  "RequestID" "421401248")
        #_(a/submit-page :credit)
        #_(eu/node->map))

    )


  ;;Submit custoemr identitiy
  (do
    (->
      (a/get-page :customer)
      (eu/node->map)

      (build-customer-request)
      (assoc "RequestID" "421401845")
      ; (dissoc "RequestID" )
      (a/submit-page :customer)
      (eu/node->map)))












  (client/get
    "https://green-1.commerzfinanz.com/front?controller=CreditApplication&action=DispoMaterialType&ps=DISPOV2"
    {:cookie-store cs})






  (client/get "https://green-1.commerzfinanz.com/ratanet/front?controller=CreditApplication&action=Login&pstyle=ratanet")

  (clojure.pprint/pprint (clj-http.cookies/get-cookies cs))

  (client/get
    "https://green-1.commerzfinanz.com/ratanet/front?controller=CreditApplication&action=DispoMaterialType"
    {:cookie-store cs})

  )
;


;(System/getProperties)








