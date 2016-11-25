(ns ring.middleware.dadysql
  (:require [clojure.tools.logging :as log]
            [dadysql.clj.fail :as f]
            [dadysql.http-service :as h]
            [clojure.stacktrace]
            [dadysql.jdbc-io :as io]
            [dadysql.jdbc :as tj]))


(defn find-module-index-by-url [old-coll url]
  (->> (map-indexed vector old-coll)
       (filter #(= (get (second %) :url) url))
       (map first)
       (first)))


(defn get-module-by-url [old-coll url]
  (->> (find-module-index-by-url old-coll url)
       (get old-coll)))


(defn assoc-module [old-coll new]
  (let [find-index (find-module-index-by-url old-coll (:url new))]
    (if (nil? find-index)
      (conj old-coll new)
      (assoc-in old-coll [find-index] new))))



(defn load-module
  [{:keys [ds file-name init-name spec-dir]
    :or   {spec-dir "target"}
    :as   m}]
  (try
    (let [v (tj/read-file file-name)]
      (do
        (log/info "Loading file " file-name)
        (when init-name
          (log/info "Doing database init " init-name)
          (io/db-do @ds (tj/select-name v {:dadysql.core/name init-name})))
        (io/validate-dml! @ds (tj/get-sql-statement v))
        (-> m
            (assoc :url (str "/" (first (clojure.string/split file-name #"\."))))
            (assoc :tms v)
            (dissoc :init-name))))
    (catch Exception e
      (clojure.stacktrace/print-stack-trace e)
      (log/info (str " File loading error " (str m)) (.getMessage e))
      nil)))



(defn try!
  [form & v]
  (try
    (apply form v)
    (catch Exception e
      (log/error e)
      (f/fail {:msg "Error in server "}))))


(defn- get-handler [type ds tms]
  (condp = type
    :pull
    (-> (partial tj/pull ds tms)
        (h/warp-pull)
        (h/warp-default))
    :push
    (-> (partial tj/push! ds tms)
        (h/warp-push)
        (h/warp-default))
    nil))

(defn- config-available? [config-atom req-url]
  (get-module-by-url @config-atom req-url))


(defn- reload-and-get-config [config-atom req-url reload?]
  (if-let [config (get-module-by-url @config-atom req-url)]
    (let [config (if reload?
                   (load-module config)
                   config) ]
      (when config
        (swap! config-atom (fn [v] (assoc-module v config)))
        config))))





;ds-atom tms-atom
(defn warp-dadysql-handler
  "Warper that tries to do with dadysql. It should use next to the ring-handler. If path-in is matched with
   pull-path or push-path then it will API and return result.

   handler: Ring handler
   ds-atom: Clojure datasource as atom
   tms-atom: dadysql file as atom
   pull-path and push path string

  "
  [handler config-atom & {:keys [pull-path push-path log? encoding reload?]
                          :or   {pull-path "/pull"

                                 push-path "/push"
                                 reload? true}}]
  (fn [req]
    (let [request-path (clojure.string/split (or (:path-info req)
                                                 (:uri req)) #"/")
          req-url (clojure.string/join "/" (butlast request-path))]
      (if (config-available? config-atom req-url)
        (let [config (reload-and-get-config config-atom req-url reload?)
              ds (or (:ds req) (:ds config))
              tms (or (:tms req) (:tms config))
              op (str "/" (last request-path))]
          (condp = op
            pull-path
            ((get-handler :pull @ds tms) req)
            push-path
            ((get-handler :push @ds tms) req)
            (handler req)))
        (handler req)))))



(defn get-sql-file-value [tms-atom]
  (->> @tms-atom
       (vals)
       (mapv (fn [w] (select-keys w [:dadysql.core/name :dadysql.core/model :dadysql.core/sql])))))








