 (ns dev-system
   (:require
     [figwheel-sidecar.system :as sys]
     [com.stuartsierra.component :as c]
     [tiesql.server :as a]))


(defrecord FigSystem [app fig]
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


(defn init-system []
  (-> (c/system-map
        :figwheel-system (map->FigSystem {})
        :app (a/app-system "tiesql.edn"))
      (c/system-using {:figwheel-system {:app :app}})))
