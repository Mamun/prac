(ns dadysql.http-request
  (:require [dady.common :as c]
            [dady.fail :as f]
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



(defmulti request-format (fn [t _] t))


(defmethod request-format u/api-endpoint
  [_ params]
  (-> params
      (update-in [u/dadysql-name] c/as-keyword-batch)
      (filter-nil-value)))


(defmethod request-format u/url-endpoint
  [_ params]
  (let [r-params (dissoc params u/dadysql-name :dadysql.core/output-format :dadysql.core/param-format :gname)
        q-name (c/as-keyword-batch (u/dadysql-name params))
        other (-> params
                  (select-keys [:gname :dadysql.core/output-format :dadysql.core/param-format])
                  (as-keyword-value))]
    (-> other
        (assoc :dadysql.core/name q-name)
        (assoc :params r-params)
        (filter-nil-value))))


(defn param-keywordize-keys
  [req]
  (if (= :string (:input req))
    (assoc req :params (clojure.walk/keywordize-keys (:params req)))
    req))


(defn is-params-not-nil?
  [params]
  (if params
    params
    (f/fail "No params is set in http request")))


(defn as-request
  [ring-request type]
  (f/try->> ring-request
            (:params)
            (is-params-not-nil?)
            (request-format type)
            (param-keywordize-keys)))


