(ns user
  (:require [app.core :as app]
            [dadysql.jdbc :as j]
            [ring.middleware.reload :refer [wrap-reload]]
            [figwheel-sidecar.repl-api :as figwheel]))


(def http-handler
  (wrap-reload #'app/http-handler))

(defn run []
  (app/init-state)
  (figwheel/start-figwheel!))

(defn cljs-repl [] (figwheel/cljs-repl))


(comment



  (spit
    "./target/tie.cljc"
    (-> (j/read-file "tie.edn.sql")
        (j/get-spec-str "target.core")))





  (cljs-repl)
  (run)


  )