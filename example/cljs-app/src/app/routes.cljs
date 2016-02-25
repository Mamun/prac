(ns app.routes
  (:require [secretary.core :as secretary]
            [tiesql.history]
            [app.component :as h])
  (:require-macros [secretary.core :refer [defroute]]))


#_(defroute "/users/:id" {:as params}
     (h/dept-list "app" params))


(defroute "/users/:id" {:as params}
    (h/dept-list "app" params))


(defroute "*" []
   (js/console.log "home path "))



#_(tiesql/pull "/"
             :name :get-dept-by-id
             :params {:id 1}
             :callback (fn [v]
                         (print "------------")
                         (print v)

                         ))

;(h/nav! "/users/gf3")
;(secretary/dispatch! "/users/gf3")
;(secretary/dispatch! "/users/gf3")
;(secretary/dispatch! "/users/hello2")
;(secretary/dispatch! "/")