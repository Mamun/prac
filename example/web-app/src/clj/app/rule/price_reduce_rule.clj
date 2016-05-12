(ns app.rule.price-reduce-rule
  (:require [clara.rules :refer :all]
            [clara.rules.accumulators :as acc]))


(defrecord Customer [status])
(defrecord Order [year month day])
(defrecord Purchase [cost item])
(defrecord Total [total])
(defrecord Discount [reason percent])


(defrule total-purchase
         "Total purchase"
         [?total <- (acc/sum :cost) :from [Purchase]]
         =>
         (insert! (->Total ?total)))


(defrule total-purchase-discount
         "Discount on total purchase"
         [Total (> total 20)]
         =>
         (insert! (->Discount :total_purchase 10)))


(defquery get-total-purchase
          []
          [?total <- Total]
          [?discount <- Discount])


(comment



  (let [w (-> (mk-session)
              (insert (->Purchase 10 :hello)
                      (->Purchase 30 :hello1)
                      )
              (fire-rules)
              (query get-total-purchase))]
    w
    )

  )