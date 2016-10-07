(ns dady.spec
  (:require [clojure.spec :as s]
            [clojure.walk :as w]))





(defn namespace-keyword [n k]
  (let [n (cond (keyword? n) (name n)
                :else (symbol n))]
    (if (namespace k)
      k
      (keyword (str n "/" (name k))))))


(defn model-spec [n mk]
  (let [v (mapv (fn [v]
                  (namespace-keyword n v)
                  ) (keys mk))
        w (namespace-keyword n :spec)]
    `(s/def ~w (s/keys :req-un ~v))))


(defn model-property-spec [n mk]
  (map (fn [[k v]]
         (let [w (namespace-keyword n k)]
           `(s/def ~w ~v))
         ) mk))


(defmacro defm [n mk]
  (let [m-spec (model-spec n mk)
        m-spec-child (model-property-spec n mk)
        w (reverse (cons m-spec m-spec-child))]
    `(do ~@w)))


#_(defn make-spec [n mk]
    (defm n mk)
    )




(comment


  (model-spec :a {:a int?})


  (model-property-spec :a {:a int?})

  (defm :a {:a string?})
  (defm b {:a int?})

  (macroexpand-1 '(defm :a {:a int?}))

  (s/valid? :a/spec {:a "asdf"})

  (s/valid? :a/spec {:b "asdf"})

  (s/valid? :b/spec {:a "asdf"})



  #_(let [w {:a :a/a :b :b/b}]
      (->> {:a 2 :b [1 :a]}
           (clojure.walk/postwalk (fn [x]
                                    (if-let [v (get w x)]
                                      v
                                      x)
                                    ))))


  (str :hello)
  (find-ns-spec :dadysql)

  (find-ns-spec 'get-dept-by-id)

  (require '[clojure.spec :as s])

  (s/registry)



  (defsp
    hello6.hello6
    (s/def ::b int?)
    (s/def ::c (s/keys :req-un [::b]))

    )

  ;(str *ns*)
  ;;usage keyword will be with full name
  (macroexpand-1 '(defsp
                    hello2.ghello
                    ;  (require '[clojure.spec :as spec])
                    (s/def ::b string?)
                    (s/def ::c (s/keys :req-un [:t/b]))
                    ))

  )



(comment

  (aa int?)



  #_(->> (map-namespace-key 'defm {:id int? :hello string?})
         (model-spec 'defm)
         )



  (macroexpand-1 '(defm :hello {:id int? :hello string?}))



  (build-id :hello {:id int? :hello string?})




  (keys
    {:id int?})


  #_(s/valid?
      (::s/kvs->map {:id int?})
      {:id "hello"}
      )

  ;(s/form :get-dept-by-ids/id)
  ;(ds/find-ns-spec 'cfg )

  (s/registry)

  {:id int? :vip string?}

  ;(resolve 'int1?)

  (s/conform ::int "asdfsd")

  (s/conform :get-dept-by-id/spec {:id 2})

  (s/spec? (s/spec ::get-dept-by-id))

  ;(s/valid? keyword? ::a)

  (load-file "tie_edn.clj")

  )