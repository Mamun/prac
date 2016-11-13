(ns dadysql.clj.spec-gen-impl
  (:require [clojure.spec :as s]))

(defn var->symbol [v]
  (if (var? v)
    (symbol (clojure.string/replace (str v) #"#'" ""))
    v))



(defn convert-property-to-def [m]
  (map (fn [[k v]]
         (list 'clojure.spec/def k v))
       m))

(defn add-postfix-to-key [k v]
  (if (namespace k)
    (keyword (str (namespace k) "/" (name k) v))
    (keyword (str (name k) v))))


(defn build-key [req opt qualified?]
  (let [req-key (if qualified? :req :req-un )
        opt-key (if qualified? :opt :opt-un )
        w ['clojure.spec/keys]
        w (if (not-empty req)
            (into w [req-key (into [] (keys req))])
            w)
        w (if (not-empty opt)
            (into w [opt-key (into [] (keys opt))])
            w)
        w (apply list w)]
    w))

;; or does not work correctly for unfrom core api
(defn convert-model-tp-def
  [k req opt]
  (let [                                                    ;w (build-key req opt true )
        w-un (build-key req opt false )]
    `((clojure.spec/def ~k ~w-un
        #_(clojure.spec/or :qualified ~w
                         :unqualified ~w-un))
       (clojure.spec/def ~(add-postfix-to-key k "-list")
         (clojure.spec/coll-of ~k :kind vector?)))))


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


(defn model->spec2 [[model-k model-v]]
  (let [req-k (assoc-ns-key model-k (:req model-v))
        opt-k (assoc-ns-key model-k (:opt model-v))
        w (convert-property-to-def (merge req-k opt-k))]
    (->> (into w (convert-model-tp-def model-k req-k opt-k))
         (reverse))))