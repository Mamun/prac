(ns user
  (:use dev-system)
  (:require
    [figwheel-sidecar.system :as sys]
    [com.stuartsierra.component :as c]
    [clojure.tools.namespace.repl :as r]))


(def system nil)

(defn init []
  (alter-var-root #'system (constantly (init-system))))


(defn start []
  (alter-var-root #'system c/start))


(defn stop []
  (alter-var-root #'system c/stop))


(defn repl []
  (sys/cljs-repl (get-in system [:figwheel-system :fig])))


(defn go []
  (do
    (init)
    (start)))


(defn reset []
  (stop)
  (r/refresh :after 'user/go))




(comment

  ;(amap2 {:b 4})

  (start)

  (-> (sys/fetch-config)
      (clojure.pprint/pprint))

  (stop)

  (repl)

  (-> {:a 3}
      (update-in [:a] (fnil identity 5))
      )

  )