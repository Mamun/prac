(ns dady.spec
  (:require [clojure.spec :as spec]
            [clojure.walk :as w]))


(defn update-ns
  "Doc "
  [ns-str spec-list]
  (w/postwalk (fn [v]
                (if (and (keyword? v)
                         (= (namespace v) (str *ns*)))
                  (keyword (str ns-str "/" (name v)))
                  v)
                ) spec-list))


(defmacro defsp
  "Doc "
  [n & content]
  (let [content (update-ns n content)]
    `(do
       ;     (clojure.core/remove-ns '~(symbol n))
       (clojure.core/in-ns '~(symbol n))
       (clojure.core/refer 'clojure.core)
       (clojure.core/require '[clojure.spec :as ~(symbol 's)])
       ~@content
       nil)))






(defn find-ns-spec [ns-name]
  (->>
    (filter (fn [w]
              (let [[k _] w]
                (clojure.string/includes? (str k) (str ns-name)))
              ) (spec/registry))
    (into {})))




(comment


  #_(let [w {:a :a/a :b :b/b}]
      (->> {:a 2 :b [1 :a]}
           (clojure.walk/postwalk (fn [x]
                                    (if-let [v (get w x)]
                                      v
                                      x)
                                    ))))


  (str :hello)
  (find-ns-spec :dadysql)

  (find-ns-spec 'get-dept-by-id)

  (require '[clojure.spec :as spec])

  (spec/registry)



  (defsp
    hello6.hello6
    (s/def ::b int?)
    (s/def ::c (s/keys :req-un [::b]))

    )

  ;(str *ns*)
  ;;usage keyword will be with full name
  (macroexpand-1 '(defsp
                    hello2.ghello
                    ;  (require '[clojure.spec :as spec])
                    (s/def ::b string?)
                    (s/def ::c (s/keys :req-un [:t/b]))
                    ))

  )