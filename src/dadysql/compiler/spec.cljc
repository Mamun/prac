(ns dadysql.compiler.spec
  (:require [clojure.spec :as s]))


(s/def :dadysql.core/common
  (s/merge
    (s/keys :opt [:dadysql.core/timeout
                  :dadysql.core/column
                  :dadysql.core/result
                  :dadysql.core/param-coll
                  :dadysql.core/param-spec])))



(s/def :dadysql.core/extend
  (s/every-kv keyword? (s/merge (s/keys :opt [:dadysql.core/model]) :dadysql.core/common)))



(comment

  (s/exercise :dadysql.core/common)

  (clojure.pprint/pprint
    (s/exercise :dadysql.core/extend 1))

  )



(s/def :dadysql.core/module (s/merge
                              :dadysql.core/common
                              (s/keys :req [:dadysql.core/name :dadysql.core/sql]
                                      :opt [:dadysql.core/model :dadysql.core/skip :dadysql.core/group :dadysql.core/commit :dadysql.core/extend])))


(s/def :dadysql.core/spec-file symbol?)

(s/def :dadysql.core/global (s/keys :req [:dadysql.core/name]
                                    :opt [:dadysql.core/timeout :dadysql.core/read-only? :dadysql.core/tx-prop :dadysql.core/file-reload :dadysql.core/reserve-name :dadysql.core/join :dadysql.core/spec-file]))


(s/def :dadysql.core/compiler-spec
  (clojure.spec/cat
    :global (s/? :dadysql.core/global)
    :module (s/* :dadysql.core/module)))




(defn validate-input-spec! [coll]
  (let [w (s/conform :dadysql.core/compiler-spec coll)]
    (if (= w :clojure.spec/invalid)
      (do
        (println (s/explain-str :dadysql.core/compiler-spec coll))
        (throw (ex-info "Compile failed " (s/explain-data :dadysql.core/compiler-spec coll)))))))







