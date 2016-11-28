(ns productivity-spec
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [clojure.spec.test :as stest]))


(s/def :prac/in (s/+ (s/coll-of string? :type vector? :distinct true)))


(defn combine-two [f s]
  (for [f1 f
        s1 s]
    (str f1 s1)))


(defn combine-batch [in]
  (reduce combine-two in))


#_(defn return-count [in]
  (apply * (map count (take 2 (reverse (sort-by count in))))))


(s/fdef combine-batch
        :args (s/cat :in :prac/in)
        :ret coll?
        ;:fn #(= (count (:ret %)) (return-count (-> % :args :r)))
        )



(comment

  (stest/check `combine-batch)
  (stest/instrument `combine-batch)

  (clojure.repl/doc combine-batch)

  (s/exercise :prac/in)
  (gen/sample (s/gen :prac/in) 3)


  ;(return-count [["s"] ["1" "2"] ["a" "b" "c"] ["r" "p" "q" "o"] ] )

  (combine-batch (list ["s"] ["1" "2"] ["a" "b" "c"]) )

  (combine-batch
    (list ["2S" "r" "" "TIF" "s" "7" "O" "9r4"]
          ["VPPz" "K" "hli" "1j" "h2c" "R" "f" "HZ" "u" "" "D" "5m9" "dtl" "f0"]) )

  )

