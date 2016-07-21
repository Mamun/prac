(ns dadysql.constant
  (:use [dady.common]
        [dady.fail]))


(defonce global-key :_global_)
(defonce module-key :_module_)
(defonce process-context-key :process-context)
(defonce reserve-name-key :reserve-name)
(defonce file-name-key :file-name)
(defonce file-reload-key :file-reload)
(defonce ds-key :datasource)
(defonce ds-conn-key :datasource-conn)
(defonce tx-prop :tx-prop)


(defonce name-key :name)
(defonce column-key :column)
(defonce doc-key :doc)
(defonce model-key :model)
(defonce skip-key :skip)
(defonce timeout-key :timeout)
(defonce group-key :group)
(defonce index :index)
(defonce sql-key :sql)


;(def root-meta :meta)
(defonce extend-meta-key :extend)

;(def meta-with-extend #{root-meta extend-meta-key})

(def nested-map-format :nested)
(def nested-array-format :nested-array)
(def nested-join-format :nested-join)
(def map-format :map)
(def array-format :array)
(def value-format :value)


(defonce output-key :output)
(defonce input-key :input)


(defonce result-key :result)
(defonce result-array-key :array)
(defonce result-single-key :single)

;(def in-type :input-type)
;(def out-type :output-type)


(defonce param-key :param)
(defonce param-ref-con-key :ref-con)
(defonce param-ref-key :ref-key)
(defonce param-ref-fn-key :ref-fn-key)
(defonce param-ref-gen-key :ref-gen)


(defonce param-spec-key :param-spec)
#_(defonce validation-type-key :type)
#_(defonce validation-range-key :range)
#_(defonce validation-contain-key :contain)


(defonce join-key :join)
(defonce join-1-1-key :1-1)
(defonce join-1-n-key :1-n)
(defonce join-n-1-key :n-1)
(defonce join-n-n-key :n-n)


(defonce dml-key :dml-type)
(defonce dml-select-key :select)
(defonce dml-insert-key :insert)
(defonce dml-update-key :update)
(defonce dml-delete-key :delete)
(defonce dml-call-key :call)



(defonce commit-key :commit)
(defonce commit-all-key :all)
(defonce commit-any-key :any)
(defonce commit-none-key :none)


;(defonce error-key :error)
(defonce exec-time-total-key :exec-total-time)
(defonce exec-time-start-key :exec-start-time)
(defonce query-exception-key :query-exception)


(def all-oformat #{nested-join-format nested-map-format nested-array-format
                   map-format array-format value-format})
(def all-pformat #{nested-map-format map-format})











(defn validate-input!
  [{:keys [gname name params oformat pformat] :as request-m}]
  (cond
    (and (nil? name)
         (nil? gname))
    (fail "Need value either name or gname")
    (and (not (nil? gname))
         (not (keyword? gname)))
    (fail "gname will be keyword")
    (and (not (nil? name))
         (not (sequential? name))
         (not (keyword? name)))
    (fail "name will be keyword")
    (and
      (sequential? name)
      (not (every? keyword? name)))
    (fail "name will be sequence of keyword")
    (and                                                    ;(= map-format out-format)
      (sequential? name)
      (contains? #{map-format array-format value-format} oformat))
    (fail #?(:clj  (format "only one name keyword is allowed for %s format " oformat)
             :cljs "Only one name keyword is allowed"))
    (and
      (not (nil? params))
      (not (map? params)))
    (fail "params will be map format ")
    (and
      (not (nil? pformat))
      (not (contains? all-pformat pformat)))
    (fail #?(:clj  (format "pformat is not correct, it will be %s ", (str all-pformat))
             :cljs "pformat is not correct"))
    (and
      (not (nil? oformat))
      (not (contains? all-oformat oformat)))
    (fail #?(:clj  (format "oformat is not correct, it will be %s" (str all-oformat))
             :cljs "oformat is not correct"))
    :else
    request-m))
