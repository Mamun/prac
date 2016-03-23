(ns app.rregister
  (:require-macros [reagent.ratom :refer [reaction]]
                   [secretary.core :refer [defroute]])
  (:require                                                 ;[secretary.core :as secretary]
    ;[pushy.core :as pushy]
    [tiesql.util :as u]
    [re-frame.core  :refer [register-handler
                            trim-v
                           path
                           register-sub
                           dispatch
                           dispatch-sync
                           subscribe]]
    [tiesql.client :as client]))


#_(defroute "*" [] (js/console.log "home path "))
#_(secretary/set-config! :prefix "#")
#_(def history (pushy/pushy secretary/dispatch!
                          (fn [x] (when (secretary/locate-route x) x))))

;; Start event listeners
#_(pushy/start! history)



(register-handler
  :pull-merge
  (fn [db [_ [v e]]]
    (if v
      (update-in db [:data] (fn [_] v))
      (update-in db [:data] (fn [_] e)))))



(defn pull-handler [db [p]]
  (do
    (->> (conj p
               :callback
               (fn [v]
                 (dispatch [:pull-merge v])))
         (apply client/pull))
    db))


(defn removeevent [handler]
  (fn []))

(register-handler :pull trim-v pull-handler)



(register-sub
  :pull
  (fn
    [db _]                                                  ;; db is the app-db atom
    (reaction (->> (get-in @db [:data])
                   (u/postwalk-remove-nils)
                   (u/postwalk-replace-tag-value)))))




(register-handler
  :url
  (fn [db [_ v]]
    (print v)
    (assoc-in db [:url] v)))


(register-sub
  :url
  (fn [db _]
    (reaction (get-in @db [:url]))))



;(dispatch [:pull [{:a {:a 2}} nil] ])

;(map-menu-dispatch ["Department" "/pull?name=get-dept-list" [:remote-pull :name [:get-dept-list]]])

