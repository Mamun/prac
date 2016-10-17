(ns dadysql.http-util
  (:require [dady.common :as c]
            [dady.fail :as f]
            [dady.walk :as w]
            [dadysql.util :as u]
            [clojure.tools.reader.edn :as edn]))


(defn read-params-string
  [params]
  (->> params
       (reduce (fn [acc [k v]]
                 (let [v1 (edn/read-string v)]
                   (if (symbol? v1)
                     (assoc acc k v)
                     (assoc acc k v1)))
                 ) {})))



(defn response-stringify
  [req response]
  (if (= :string (:output req))
    (mapv (partial w/postwalk-replace-key-with w/keyword->str) response)
    response))




(defn as-keyword-value
  [m]
  (into {}
        (for [[k v] m]
          [(keyword k) (keyword v)])))


(defn filter-nil-value
  [m]
  (->> m
       (filter (comp not nil? val))
       (into {})))


(defn param-keywordize-keys
  [req]
  (if (= :string (:input req))
    (assoc req :params (clojure.walk/keywordize-keys (:params req)))
    req))



(defn as-str [v]
  (cond
    (string? v) v
    (keyword? v) (name v)
    (number? v) (str v)
    :else v))



