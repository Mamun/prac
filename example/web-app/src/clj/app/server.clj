(ns app.server
  (:require [immutant.web :as im]
            [clojure.tools.logging :as log]
            [app.state :as s]
            [app.routes :as r])
  (:gen-class))



(defn -main
  [& args]
  (println "Starting tie app  ")
  (s/init-state)
  (im/run r/http-handler {:port 3000
                        ;:host "0.0.0.0"
                        }))


(comment


  (im/run http-handler {:port 3000
                        ;:host "0.0.0.0"
                        })

  )