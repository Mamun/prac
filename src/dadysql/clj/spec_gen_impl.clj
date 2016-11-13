(ns dadysql.clj.spec-gen-impl
  (:require [clojure.spec :as s]))

(defn var->symbol [v]
  (if (var? v)
    (symbol (clojure.string/replace (str v) #"#'" ""))
    v))


(defn resolve-symbol
  [v]
  (if (and (seq? v)
           (= 'quote (first v)))
    (let [w (eval v)]
      (cond
        (symbol? w) (if-let [r (resolve w)]
                      r
                      (throw (ex-info "Could not resolve symbol " {:symbol (eval v)}))
                      )
        :else w))
    v))


(defn convert-property-to-def [m]
  (map (fn [[k v]]
         (list 'clojure.spec/def k v))
       m))


(defmulti convert-model-tp-def (fn [k req opt t] t))

(defmethod convert-model-tp-def
  :default
  [k req opt _]
  (let [w ['clojure.spec/keys]
        w (if (not-empty req)
            (into w [:req (into [] (keys req))])
            w)
        w (if (not-empty opt)
            (into w [:opt (into [] (keys opt))])
            w)
        w (apply list w)]
    (list 'clojure.spec/def k
          (list 'clojure.spec/or
                :one w
                :list (list 'clojure.spec/coll-of w :kind 'vector?)))))

(def un-postfix "-un")

(defn add-postfix-to-key [k v]
  (if (namespace k)
    (keyword (str (namespace k) "/" (name k) v))
    (keyword (str (name k) v))))


(defmethod convert-model-tp-def
  un-postfix
  [k req opt _]
  (let [w-un ['clojure.spec/keys]
        w-un (if (not-empty req)
               (into w-un [:req-un (into [] (keys req))])
               w-un)
        w-un (if (not-empty opt)
               (into w-un [:opt-un (into [] (keys opt))])
               w-un)
        w-un (apply list w-un)]
    (list 'clojure.spec/def (add-postfix-to-key k un-postfix)
          (list 'clojure.spec/or
                :one w-un
                :list (list 'clojure.spec/coll-of w-un :kind 'vector?)))))



(defn create-ns-key [ns-key r]
  (let [w (if (namespace ns-key)
            (str (namespace ns-key) "." (name ns-key))
            (name ns-key))]
    (if (namespace r)
      (keyword (str w "." (namespace r) "/" (name r)))
      (keyword (str w "/" (name r))))))


(defn assoc-ns-key [ns-key m]
  (if (nil? m)
    {}
    (->> (map (fn [v]
                (create-ns-key ns-key v)) (keys m))
         (interleave (keys m))
         (apply assoc {})
         (clojure.set/rename-keys m))))

(defn model->spec2 [r [model-k model-v]]
  (let [req-k (assoc-ns-key model-k (:req model-v))
        opt-k (assoc-ns-key model-k (:opt model-v))
        w (convert-property-to-def (merge req-k opt-k))]
    (->> r
         (reduce (fn [acc v]
                   (cons (convert-model-tp-def model-k req-k opt-k v) acc )
                   ) w)
         (reverse))))