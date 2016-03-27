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



(def model-employee  :model-employee)


(register-handler
  model-employee
  (fn [db [_  [v e]]]
    (update-in db [model-employee] (fn [_] (or v e)))))


(register-sub
  model-employee
  (fn [db _]
    (reaction (-> (get-in @db [model-employee])
                  (select-keys [:employee])
                  (tu/postwalk-remove-nils)
                  (tu/postwalk-replace-tag-value)))))

#_(defn- get-employee-by-id [id]
  (client/pull
    :gname :load-employee
    :params {:id id}
    :callback (fn [v] (dispatch [model-employee v]))))


#_(register-handler
  :employee-search
  (fn [db [_ id]]
    (do
      (get-employee-by-id id)
      db)))


;(dispatch [:employee-list])

