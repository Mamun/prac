(ns app.routes
  (:require [secretary.core :as secretary]
            [tiesql.client :as tiesql]
            [tiesql.history :as h]
            [app.component :as c])
  (:require-macros [secretary.core :refer [defroute]]))



(defroute "/users/:id" {:as params}
   (do
     (tiesql/pull "/"
                  :name [:get-dept-list]
                  :callback (fn [w]
                              (let [[v e] w
                                    d (atom (:department v))]
                                (c/show-counter d ))


                              (print "------------")


                              ))))


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