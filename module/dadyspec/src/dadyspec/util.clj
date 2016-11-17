(ns dadyspec.util)


(defn add-postfix-to-key [namespace-key post-fix-str]
  (if post-fix-str
    (if (namespace namespace-key)
      (keyword (str (namespace namespace-key) "/" (name namespace-key) post-fix-str))
      (keyword (str (name namespace-key) post-fix-str)))
    namespace-key))


;(namespace :a.a)

;; or does not work correctly for unfrom core api
(defn as-ns-keyword [ns-key r]
  ;(println ns-key "--" r)

  (let [w (if (namespace ns-key)
            (str (namespace ns-key) "." (name ns-key))
            (name ns-key))]
    (if (namespace r)
      (keyword (str w "." (namespace r) "/" (name r)))
      (keyword (str w "/" (name r))))))



(defn rename-key-to-namespace-key [namespace-key m]
  (if (nil? m)
    {}
    (->> (map (fn [v]
                (as-ns-keyword namespace-key v)) (keys m))
         (interleave (keys m))
         (apply assoc {})
         (clojure.set/rename-keys m))))


(defn update-model-key-one [model-k model-property]
  (as-> model-property m
        (if (:req m)
          (update m :req (fn [w] (rename-key-to-namespace-key model-k w)))
          m)
        (if (:opt m)
          (update m :opt (fn [w] (rename-key-to-namespace-key model-k w)))
          m)))




(comment

  #_(rename-model-key-to-namespace-key {:student {:req {:id :a}}} :app)

  )



(defn get-spec-model [base-ns-name m]
  (let [w (-> (as-ns-keyword base-ns-name :spec)
              (rename-key-to-namespace-key m)
              (keys))]
    (->> (mapv #(add-postfix-to-key % "-list") w)
         (concat w))))



(comment

  (get-spec-model :app {:student {:req {:id :a}}
                        :dept    {:req :s}
                        })
  )



(defn reverse-join [[src rel dest]]
  (condp = rel
    :dadyspec.core/rel-one-one [dest :dadyspec.core/rel-one-one src]
    :dadyspec.core/rel-many-one [dest :dadyspec.core/rel-one-many src]
    :dadyspec.core/rel-one-many [dest :dadyspec.core/rel-many-one src]))



(defn assoc-ns-join [base-ns-name [src rel dest]]
  (condp = rel
    :dadyspec.core/rel-one-one (as-ns-keyword base-ns-name dest)
    :dadyspec.core/rel-many-one (as-ns-keyword base-ns-name dest)
    :dadyspec.core/rel-one-many (-> (as-ns-keyword base-ns-name dest)
                                    (add-postfix-to-key "-list")))
  )



#_(defn rename-join-key-to-ns-key [namespace-name join]
    (->> join
         (mapv reverse-join)
         (into join)
         (distinct)
         (mapv #(assoc-ns-join namespace-name %))
         (group-by first)))


(comment

  (rename-join-key-to-ns-key :hello [[:a :dadyspec.core/rel-many-one :b]])
  )