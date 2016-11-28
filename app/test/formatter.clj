 (ns formatter)

(def FieldNo "FieldNo")
(def FieldContent "FieldContent")

;;;;;;;;;;;;;;;;;;; Start mapping ;;;;;;;;;;;;;;;;;

(defn is-block-140-and-content-sc? [block-of-lines]
  (reduce (fn [acc v]
            (if (and (= 140 (get v FieldNo))
                     (= "SC" (get v FieldContent)))
              (reduced true)
              acc)
            ) false block-of-lines))


(defn is-block-140-and-content-kr? [block-of-lines]
  (reduce (fn [acc v]
            (if (and (= 140 (get v FieldNo))
                     (not= "SC" (get v FieldContent))
                     (not= "ST" (get v FieldContent)))
              (reduced true)
              acc)
            ) false block-of-lines))


(defn is-block-110-and-content-p? [block-of-lines]
  (reduce (fn [acc v]
            (if (and (= 110 (get v FieldNo))
                     (= "P" (get v FieldContent)))
              (reduced true)
              acc)
            ) false block-of-lines))


(defn map-personal-info-one [person-block-of-lines]
  (reduce (fn [acc v]
            (condp = (get v FieldNo)
              110 (assoc acc :feild-100 (get v "FieldContent"))
              121 (assoc acc :first-name (get v "FieldContent"))
              122 (assoc acc :last-name (get v "FieldContent"))
              123 (assoc acc :gender (get v "FieldContent"))
              124 (assoc acc :birth-date (get v "FieldContent"))
              128 (assoc acc :address (get v "FieldContent"))
              152 (assoc acc :plz (get v "FieldContent"))
              acc)
            ) {} person-block-of-lines))


(defn map-score-one [score-block-of-lines]
  (reduce (fn [acc v]
            (condp = (get v FieldNo)
              110 (assoc acc :feild-100 (get v "FieldContent"))
              140 (assoc acc :score-code (get v "FieldContent"))
              157 (assoc acc :score-wert (get v "FieldContent"))
              158 (assoc acc :risiko-quote (get v "FieldContent"))
              159 (assoc acc :score-bereich (get v "FieldContent"))
              150 (assoc acc :score-text (get v "FieldContent"))
              acc)
            ) {} score-block-of-lines))


(defn map-schufa-credit-one [credit-block-of-line]
  (reduce (fn [acc v]
            (condp = (get v FieldNo)
              110 (assoc acc :feild-100 (get v "FieldContent"))
              140 (assoc acc :merkmal-code (get v "FieldContent"))
              141 (assoc acc :waehrung (get v "FieldContent"))
              142 (assoc acc :betrag (get v "FieldContent"))
              143 (assoc acc :datum (if (or (= nil (get v "FieldContent"))
                                            (= "00000000") (get v "FieldContent"))
                                      "01010001"
                                      (get v "FieldContent")
                                      ))
              145 (assoc acc :raten-zahl (get v "FieldContent"))
              146 (assoc acc :raten-art (get v "FieldContent"))
              147 (assoc acc :konto-nummer (get v "FieldContent"))
              acc)
            ) {} credit-block-of-line))



(defn do-line-format [w]
  (let [score-info (->> w
                        (filter is-block-140-and-content-sc?)
                        (map map-score-one))
        credit-info (->> w
                         (filter is-block-140-and-content-kr?)
                         (map map-schufa-credit-one))
        personal-info (->> w
                           (filter is-block-110-and-content-p?)
                           (map map-personal-info-one)
                           (first))]
    {:schufa-person personal-info
     :schufa-credit credit-info
     :schufa-score  score-info}))







;;;; End mapping ;;;;;;;;;;;;;;;;;;