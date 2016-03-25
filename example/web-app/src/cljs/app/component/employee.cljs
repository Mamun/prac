(ns app.component.employee
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent-forms.core :refer [bind-fields init-field value-of]]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]
            [tiesql.util :as tu]
            [tiesql.client :as client]))


(def m-list :model-employee-list)
(def m-one  :model-employee-one)
;(def path-employee-list [model-list])


(register-handler
  m-list
  (fn [db [_ [v e]]]
    (update-in db [m-list] (fn [_] (or v e)))))


(register-sub
  m-list
  (fn [db _]
    (reaction (-> (get-in @db [m-list])
                  (:employee)
                  (tu/postwalk-replace-tag-value)))))



(defn get-employee-list []
  (client/pull
    :name [:get-employee-list]
    ;:params {:id id}
    :callback (fn [v]
                (do
                  (print v)
                  (dispatch [m-list v])))))


(get-employee-list)



(register-handler
  m-one
  (fn [db [_  [v e]]]
    (update-in db [m-one] (fn [_] (or v e)))))


(register-sub
  m-one
  (fn [db _]
    (reaction (-> (get-in @db [m-one])
                  (select-keys [:employee])
                  (tu/postwalk-remove-nils)
                  (tu/postwalk-replace-tag-value)))))

(defn- get-employee-by-id [id]
  (client/pull
    :gname :load-employee
    :params {:id id}
    :callback (fn [v] (dispatch [m-one v]))))


(register-handler
  :employee-search
  (fn [db [_ id]]
    (do
      (get-employee-by-id id)
      db)))


;(dispatch [:employee-list])

