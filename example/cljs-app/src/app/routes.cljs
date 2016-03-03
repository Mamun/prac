(ns app.routes
  (:require [secretary.core :as secretary]
            [app.component :as h]
            [pushy.core :as pushy])
  (:require-macros [secretary.core :refer [defroute]]))


;(defroute "/users/:id" {:as params} #_(h/pull-dept))
(defroute "*" [] (js/console.log "home path "))

(secretary/set-config! :prefix "/")

(def history (pushy/pushy secretary/dispatch!
                          (fn [x] (when (secretary/locate-route x) x))))

;; Start event listeners
(pushy/start! history)


;(h/nav! "/users/gf3")
;(secretary/dispatch! "/users/gf3")
;(secretary/dispatch! "/users/gf3")
;(secretary/dispatch! "/users/hello2")
;(secretary/dispatch! "/")