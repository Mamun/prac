 (ns combi.comb-impl)


(defn combine-two [f-coll s-coll]
  (for [f1 f-coll
        s1 s-coll]
    (str f1 s1)))


(defn combine-batch [r]
  (reduce combine-two r))


(comment

  ;"a" "abc bcn" "dca 123"

  (combine-batch [["s"] ["1" "2"] ["a" "b" "c"]  ]   )

  )


