(ns dadysql.core-util
  (:require [dadysql.constant :refer :all]
            [dady.proto :refer :all]
            [dady.fail :as f]
            [dady.common :as cc]
            [dadysql.plugin.join.join-impl :as j]))


(defn validate-name!
  [tm name-coll]
  (let [skey (into #{} (keys tm))
        op-key (into #{} name-coll)]
    (if (clojure.set/superset? skey op-key)
      tm
      (->> (clojure.set/difference op-key skey)
           (first)
           (str "Name not found ")
           (f/fail)))))


(defn validate-model!
  [tm-coll]
  (let [model-coll (mapv model-key tm-coll)
        m (distinct model-coll)]
    (if (not= (count model-coll)
              (count m))
      (f/fail (str "Selecting duplicate model " model-coll))
      tm-coll)))



(defn filter-join-key
  [coll]
  (let [model-key-coll (mapv model-key coll)
        p (comp
            (cc/xf-skip-type #(= dml-call-key (dml-key %)))
            (map #(update-in % [join-key] j/filter-join-key-coll model-key-coll)))]
    (transduce p conj [] coll)))


(defn is-reserve?
  [tms coll]
  (if (->> (clojure.set/intersection
             (into #{} coll)
             (get-in tms [global-key reserve-name-key]))
           (not-empty))
    true
    false))



(defn empty-path
  []
  [[]])


(defn conj-index
  [data c-path]
  (let [path-value (get-in data c-path)]
    (if (sequential? path-value)
      (->> (count path-value)
           (range 0)
           (mapv #(conj c-path %)))
      [c-path])))


(defn get-path
  ([data name]
   (get-path data (empty-path) name))
  ([data p-path-coll name]
   (for [c-path p-path-coll
         i-path (conj-index data c-path)
         :let [n-path (conj i-path name)]
         w (conj-index data n-path)]
     w)))


#_(defn get-key-path
    [m & [p]]
    (if p
      (->> (get-in m p)
           (keys)
           (map (fn [v] (conj p v))))
      (->> (keys m)
           (map (fn [v] [v])))))


#_(defn get-key-path-with-child
    [m ck]
    (let [pp (get-key-path m)
          p (comp
              (map (fn [w] (conj w ck)))
              (filter #(get-in m %))
              (mapcat #(get-key-path m %)))]
      (->> (into [] p pp)
           (concat pp))))

#_(deftest get-key-path-with-child-test
           (testing "test get-key-path-with-child "
                    (let [v {:param {:k-order 1
                                     :k-name  :params
                                     :childs  {:ref-con {:k-name :ref-conn}}}
                             :valie {:k-order 1
                                     :k-name  :valie
                                     :childs  {:ref-con {:k-name :t}}}}
                          expected-result (list [:param]
                                                [:valie]
                                                [:param :childs :ref-con]
                                                [:valie :childs :ref-con])
                          actual-result (get-key-path-with-child v :childs)]
                      (is (= actual-result
                             expected-result)))))
