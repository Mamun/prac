(ns user
  (:require
    [figwheel-sidecar.system :as sys]
    [com.stuartsierra.component :as c]
    [clojure.tools.namespace.repl :as r]
    [figwheel-sidecar.system :as sys]
    [tools.server :as a]))


(defrecord FigRecord [app fig]
  c/Lifecycle
  (start [component]
    (if fig
      component
      (let [w (-> (sys/fetch-config)
                  (assoc-in [:figwheel-options :ring-handler] (get-in app [:routes]))
                  (assoc-in [:css-watcher] (sys/css-watcher {:watch-paths ["resources/public/css"]}))
                  (sys/figwheel-system)
                  (c/start))]
        (assoc component :fig w))))
  (stop [component]
    (when fig
      (c/stop fig))
    component))


(defn dev-system []
  (-> (c/system-map
        :figwheel-system (map->FigRecord {})
        :app (a/app-system "tiesql.edn"))
      (c/system-using {:figwheel-system {:app :app}})))


(def system nil)

(defn init []
  (alter-var-root #'system (constantly (dev-system))))


(defn start []
  (alter-var-root #'system c/start))


(defn stop []
  (alter-var-root #'system c/stop))


(defn repl []
  (sys/cljs-repl (get-in system [:figwheel-system :fig] )))


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