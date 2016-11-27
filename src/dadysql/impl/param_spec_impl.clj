(ns dadysql.impl.param-spec-impl
  (:require [dadysql.clj.fail :as f]
            [clojure.spec :as s]
            [spec-model.core :as sg]
            [spec-model.util :as sgi]))


(defn filename-as-keyword [file-name-str]
  (-> (clojure.string/split file-name-str #"\.")
      (first)
      (keyword)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn as-merge-spec [spec-coll]
  (if (or (nil? spec-coll)
          (empty? spec-coll))
    spec-coll
    (->> spec-coll
         (remove nil?)
         (cons 'clojure.spec/merge))))


#_(defn as-relational-spec [[f-spec & rest-spec]]
    (list 'clojure.spec/merge f-spec
          (list 'clojure.spec/keys :req (into [] rest-spec))))


(defn get-query-spec [tms]
  (when (nil? (get-in tms [:_global_ :dadysql.core/file-name]))
    (throw (ex-info "Filename is missing" {} ))
    )

  (let [ns-identifier (filename-as-keyword (get-in tms [:_global_ :dadysql.core/file-name])
                        )

        r (->> (vals tms)
               (filter :dadysql.core/param-spec )
               (filter (fn [m] (not= (:dadysql.core/dml m)
                                     :dadysql.core/dml-insert)))
               (into {} (map (juxt :dadysql.core/name :dadysql.core/param-spec))))]

    (sg/gen-spec ns-identifier r
                 {:spec-model.core/gen-type    #{:spec-model.core/ex
                                                 :spec-model.core/un-qualified
                                                }
                  :spec-model.core/gen-list?   false
                  :spec-model.core/gen-entity? false})))


(defn get-model-spec [tms]
  (when (nil? (get-in tms [:_global_ :dadysql.core/file-name]))
    (throw (ex-info "Filename is missing" {} ))
    )
  (let [ns-identifier (filename-as-keyword (get-in tms [:_global_ :dadysql.core/file-name])
                        )

        r (->> (vals tms)
               (filter :dadysql.core/param-spec)
               (filter (fn [m] (= (:dadysql.core/dml m)
                                  :dadysql.core/dml-insert)))
               (group-by :dadysql.core/model)
               (map (fn [[k v]] {k (apply merge (mapv :dadysql.core/param-spec v))}))
               (into {}))
        kset (into #{} (keys r) )
        j (->> (get-in tms [:_global_ :spec-model.core/join])
               (remove (fn [[s _ _ d _ ]]
                         (not (and (contains? kset s)
                                   (contains? kset d)
                                   ))

                         ) )
               )
        ]

    (sg/gen-spec ns-identifier r
                 {:spec-model.core/gen-type    #{:spec-model.core/ex
                                                 :spec-model.core/un-qualified}
                  :spec-model.core/gen-list?   false
                  :spec-model.core/join        j
                  :spec-model.core/gen-entity? true})))


(defn assosc-spec-to-m [f-k m]
  (if (contains? m :dadysql.core/param-spec)
    (if (= (:dadysql.core/dml m)
           :dadysql.core/dml-insert)
      (let [w (keyword (str "ex." (name f-k) "/" (name (:dadysql.core/model m))))]
        (assoc m :dadysql.core/spec w))
      (let [w (keyword (str "ex." (name f-k) "/" (name (:dadysql.core/name m))))]
        (assoc m :dadysql.core/spec w)))
    m))

(defn gen-spec [tms]
  (let [q-spec (get-query-spec tms)
        m-spec (get-model-spec tms)]
    (into m-spec (reverse q-spec))))




(defn get-spec [tm-coll req-m]
  (condp = (:dadysql.core/op req-m)
    :dadysql.core/op-push
    (-> (map :dadysql.core/spec tm-coll)
        (first))
    (-> (map :dadysql.core/spec tm-coll)
        (doall)
        (as-merge-spec)
        (eval)
        )))


(defn validate-param-spec [param-spec req-m]
  (if (and (nil? param-spec)
           (empty? param-spec))
    req-m
    (if (s/valid? param-spec (:dadysql.core/param req-m))
      (update-in req-m [:dadysql.core/param] (fn [w] (s/conform param-spec w)))
      (f/fail (s/explain-str param-spec (:dadysql.core/param req-m)))))
  )
