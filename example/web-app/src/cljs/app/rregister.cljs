(ns app.rregister
  (:require-macros [reagent.ratom :refer [reaction]]
                   [secretary.core :refer [defroute]])
  (:require ;[secretary.core :as secretary]
            ;[pushy.core :as pushy]
            [re-frame.core :refer [register-handler
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
  :pull
  (fn [db [_ [v e]]]
    (if v
      (update-in db [:data] merge v)
      (update-in db [:data] merge e))))


(register-sub
  :pull
  (fn
    [db _]                                                  ;; db is the app-db atom
    (reaction (get-in @db [:data]))))


(register-handler
  :not-found
  (fn [db [_ v]]
    (merge (empty db) v)))




(defn tiesql-dispatch
  [[n & p]]
  (->> (conj (into [] p)
             :callback
             (fn [v]
               (dispatch [n v])))
       (apply client/pull)))


(defn map-menu-dispatch
  [[_ _ w]]
  (fn [_]
    (if (= (first w) :pull)
      (tiesql-dispatch w)
      (dispatch w))))

;(dispatch [:pull [{:a {:a 2}} nil] ])

;(map-menu-dispatch ["Department" "/pull?name=get-dept-list" [:remote-pull :name [:get-dept-list]]])

