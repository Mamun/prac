 (ns productivity)


(defn combine-two [f s]
  (for [f1 f
        s1 s]
    (str f1 s1)))


(defn combine-batch [r]
  (reduce combine-two r))


(comment

  (combine-batch [["s"] ["1" "2"] ["a" "b" "c"]  ]   )

  )


