(ns app.service
  (:require
    [clojure.tools.logging :as log]
    [app.state :as s]
    [tiesql.jdbc :as j]))


(defn load-deals []
  (let [v (j/pull @s/ds-atom @s/tms-atom {:name :get-deal-list})]
    (log/info "---" v)
    v))





(comment

  (load-deals)
  )