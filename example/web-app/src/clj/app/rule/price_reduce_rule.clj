(ns app.rule.price-reduce-rule
  (:require [clara.rules :refer :all]))

(defrecord ClientRepresentative [name client])
(defrecord SupportRequest [client level])



(defrule is-important
         "Find important support requests."
         [SupportRequest (= :high level)]
         =>
         (println "High support requested!"))


(defrule notify-client-rep
         "Find the client representative and request support."
         [SupportRequest (= ?client client)]
         [ClientRepresentative (= ?client client) (= ?name name)]
         =>
         (println "Notify" ?name "that"
                  ?client "has a new support request!"))


(comment

  (println "Hello")

  (-> (mk-session )
      (insert (->ClientRepresentative "Alice" "Acme")
              (->SupportRequest "Acme" :high1))
      (fire-rules)
      )





  )