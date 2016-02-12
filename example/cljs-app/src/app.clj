(ns app
  (:require [tools.server :as s])
  (:gen-class))


(defn -main
  [& args]
  (println "Starting tie app  ")
  (s/boot ))


