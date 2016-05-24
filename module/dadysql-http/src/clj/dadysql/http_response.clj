(ns dadysql.http-response
  (:require [dady.fail :as f]
            [dady.walk :as w]
            [dadysql.util :as u]))





(defn response-stringify
  [req response]
  (if (= :string (:output req))
    (mapv (partial w/postwalk-replace-key-with w/keyword->str) response)
    response))



(defmulti resposne-format (fn [t _] t))


(defmethod resposne-format u/api-endpoint
  [_ output]
  output)


(defmethod resposne-format u/url-endpoint
  [_ output]
  (->> output
       (w/postwalk-replace-value-with u/as-str)
       (w/postwalk-replace-key-with w/keyword->str)))


(defn as-response [type req res]
  (f/try->> res
            (response-stringify req)
            (resposne-format type))
  )



