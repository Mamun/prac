(ns dady.spec-generator
  (:require [clojure.walk :as w]
            [clojure.spec :as s]))


(defn eval-spec [& coll-v]
  (doseq [v coll-v]
    (eval v)))


(defn create-ns-key [ns-key r]
  (let [w (if (namespace ns-key)
            (str (namespace ns-key) "." (name ns-key))
            (name ns-key))]
    (if (namespace r)
      (keyword (str w "." (namespace r) "/" (name r)))
      (keyword (str w "/" (name r))))))



(defn assoc-ns-key [ns-key m]
  (->> (map (fn [v]
              (create-ns-key ns-key v)) (keys m))
       (interleave (keys m))
       (apply assoc {})
       (clojure.set/rename-keys m)))



(defn convert-property-to-def [m]
  (map (fn [[k v]]
         (list 'clojure.spec/def k v))
       m))


(defn add-postfix-to-key [k v]
  (keyword (str (namespace k) "/" (name k) v)))

(def un-postfix "-un")

(defn convert-model-tp-def [k m]
  (let [w (list 'clojure.spec/keys :req (into [] (keys (:req m)))
                :opt (into [] (keys (:opt m))))
        w-un (list 'clojure.spec/keys :req-un (into [] (keys (:req m)))
                   :opt-un (into [] (keys (:opt m))))]
    (list
      (list 'clojure.spec/def k
            (list 'clojure.spec/or
                  :one w
                  :list (list 'clojure.spec/coll-of w :kind 'vector?)))
      (list 'clojure.spec/def (add-postfix-to-key k un-postfix)
            (list 'clojure.spec/or
                  :one w-un
                  :list (list 'clojure.spec/coll-of w-un :kind 'vector?))))))



(defn convert-model-spec [[model-k model-v]]
  (let [req-k (:req model-v)
        opt-k (:opt model-v)]
    (into
      (convert-model-tp-def model-k model-v)
      (convert-property-to-def (merge opt-k req-k)))))


(defn remove-quote [m]
  (clojure.walk/prewalk (fn [v]
                          (if (var? v)
                            (symbol (clojure.string/replace (str v) #"#'" ""))
                            v)
                          ) m))


(s/def ::req (s/map-of keyword? symbol?))
(s/def ::opt (s/map-of keyword? symbol?))
(s/def ::req-or-opt (s/merge (s/or :req (s/keys :req [::req])
                                   :opt (s/keys :opt [::opt]))
                             (s/map-of #{:req :opt} any?)))

(s/def ::model (s/map-of keyword? ::req-or-opt))


(s/def ::input (s/cat :base-ns keyword? :model ::model))


#_(s/fdef map->spec :args ::input :ret any?)

(defn contains-in?
  [m ks]
  (not= ::absent (get-in m ks ::absent)))

(defn update-if-contains
  [m ks f & args]
  (if (contains-in? m ks)
    (apply (partial update-in m ks f) args)
    m))


(defn map-req-opt [[model-k m]]
  {model-k (-> m
               (update-if-contains [:opt] (fn [w] (assoc-ns-key model-k w)))
               (update-if-contains [:req] (fn [w] (assoc-ns-key model-k w))))})


(defn model->spec
  [base-ns-name m]
  (if (s/valid? ::input [base-ns-name m])
    (->> (remove-quote m)
         (assoc-ns-key base-ns-name)
         (map map-req-opt)
         (into {})
         (map convert-model-spec)
         (apply concat))
    (throw (ex-info "failed " (s/explain-data ::input [base-ns-name m])))))



(defn union-spec [spec-coll]
  (if (or (nil? spec-coll)
          (empty? spec-coll))
    spec-coll
    (->> spec-coll
         (remove nil?)
         (map (fn [w] (add-postfix-to-key w un-postfix)))
         (cons 'clojure.spec/merge))))



(defn join-spec [[f-spec & rest-spec]]
  (list 'clojure.spec/merge  f-spec
     (list 'clojure.spec/keys :req (into [] rest-spec))))




(comment

  (s/def ::a string?)
  (s/def ::b string?)

  (s/def :hello/a (s/keys :req [::a]))
  (s/def :hello/b (s/keys :req [::a]))

  (s/def :hello/z (s/merge :hello/a (s/keys :req [:hello/b]) ) )


  (s/explain-data :hello/z {::a "hello" :hello/b {::b "asdfsd"}})

  (s/explain
    (eval
      (join-spec [:hello/a :hello/b] ))
    {::a "hello" :hello/b {::a "asdfsd"}})






  (let [m {:opt {:a 2}}]
    (cond-> true
            (contains? m :opt) (update-in m [:opt] #(assoc % :b 2)))
    (contains? m :req) (update-in m [:req] #(assoc % :b 2)))
  :always m


  (model->spec :tie {:employee          {:req {:id2 #'clojure.core/int?}},
                     :get-dept-by-id
                                        {:req
                                         {:id
                                          (#'clojure.spec/coll-of
                                            #'clojure.core/int?
                                            :kind
                                            #'clojure.core/vector?)}},
                     :get-dept-employee {:req {:id #'clojure.core/int?}},
                     :create-employee   {:req {:id #'clojure.core/int?}},
                     :create-employee2  {:req {:id2 #'clojure.core/int?}}
                     })


  (clojure.pprint/pprint
    (s/exercise ::model))


  (eval-spec
    (model->spec :a/t {:person {:req {:name 'string?}
                                :opt {:lname 'string?}}})

    )






  {:person {:req {:name 'string?}
            :opt {:lname 'string?}}
   :credit {:id 'int?}}







  (s/conform :t/person-un {:name "Hello"})
  (s/conform :t/person-un [{:name "Hello"}])
  (s/conform :t/person {:t.person/name "Hello" :t.person/lname 24})
  (s/conform :t/person [{:t.person/name "Hello"}])

  )
