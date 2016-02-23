(ns app.routes
  (:require [secretary.core :as secretary]
            [tiesql.history :as h])
  (:require-macros [secretary.core :refer [defroute]]))


(secretary/set-config! :prefix "#")


(defroute "/users/:id" {:as params}
   (js/console.log (str "User: " (:id params))))


(defroute "/" []
   (js/console.log "home path "))




;(h/nav! "/users/gf3")


;(secretary/dispatch! "/users/gf3")
;(secretary/dispatch! "/users/gf3")
;(secretary/dispatch! "/users/hello2")
;(secretary/dispatch! "/")