(ns ring.middleware.dadysql
  (:require [clojure.tools.logging :as log]
            [dady.fail :as f]
            [dadysql.http-service :as h]
            [dadysql.jdbc-io :as io]
            [dadysql.jdbc :as tj]))


(defn find-index [old-coll new]
  (->> (map-indexed vector old-coll)
       (filter #(= (get (second %) :url) (:url new)))
       (map first)
       (first)))


(defn assoc-new-file [old-coll new]
  (let [find-index (find-index old-coll new)]
    (if (nil? find-index)
      (conj old-coll new)
      (assoc-in old-coll [find-index] new))))


(defn load-file-one
  [{:keys [ds file-name init-name] :as m}]
  (let [v (tj/read-file file-name)]
    (do
      (log/info "Loading file " file-name)
      (when init-name
        (io/db-do ds (tj/select-name v {:dadysql.core/name init-name})))
      (io/validate-dml! ds (tj/get-sql-statement ds))
      (-> m
          (assoc :url (str "/" (first (clojure.string/split file-name #"\."))))
          (assoc :tms v)
          (dissoc init-name)))))


(defn get-value [old-coll new]
  (->> (find-index old-coll new)
       (get old-coll)))


(defn try!
  [form & v]
  (try
    (apply form v)
    (catch Exception e
      (log/error e)
      (f/fail {:msg "Error in server "}))))


;ds-atom tms-atom
(defn warp-dadysql-handler
  "Warper that tries to do with dadysql. It should use next to the ring-handler. If path-in is matched with
   pull-path or push-path then it will API and return result.

   handler: Ring handler
   ds-atom: Clojure datasource as atom
   tms-atom: dadysql file as atom
   pull-path and push path string

  "
  [handler config-atom & {:keys [pull-path push-path log? encoding]
                          :or   {pull-path "/pull"
                                 push-path "/push"}}]
  (fn [req]
    (let [request-path (or (:path-info req)
                           (:uri req))
          r (clojure.string/split request-path #"/")
          req-url (clojure.string/join "/" (butlast r))
          config (get-value @config-atom {:url req-url})]
      (if (nil? config)
        (handler req)
        (let [config (load-file-one config)
              ds (or (:ds req) (:ds config))
              tms (or (:tms req) (:tms config))]
          (swap! config-atom (fn [v] (assoc-new-file v config)))
          (condp = (str "/" (last r))
            pull-path
            (let [handler (-> (partial tj/pull ds tms)
                              (h/warp-pull)
                              (h/warp-default))]
              (handler req))
            push-path
            (let [handler (-> (partial tj/push! ds tms)
                              (h/warp-push)
                              (h/warp-default))]
              (handler req))
            (do
              (handler req)))))))
  )



(defn get-sql-file-value [tms-atom]
  (->> @tms-atom
       (vals)
       (mapv (fn [w] (select-keys w [:dadysql.core/name :dadysql.core/model :dadysql.core/sql])))))








