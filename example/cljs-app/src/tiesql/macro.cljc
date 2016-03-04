(ns tiesql.macro)

(defmacro defcard-tiesql
  [name description & param]
  (let [v (gensym 'v)
        app-code (str param)
        description (str description
                         "

                          ```
                          "
                         app-code
                         "

                         ```
                       ")]
    `(~@param
       :callback (fn [~v]
                   (devcards.core/defcard ~name
                                          ~description
                                          ~v)))))



(defmacro dispatch-tiesql-pull
  [[hname & params]]
  (let [v (gensym 'v)]
    `(tiesql.client/pull
       ~@params
       :callback (fn [~v]
                   (re-frame.core/dispatch [~hname ~v])))))




;(macroexpand-1 '(pull :name :ge))

#_(defn pull
    [n params]
    (tiesql.client/pull :name n
                        :params params
                        :callback (fn [[model e]]
                                    (if model
                                      (re-frame.core/dispatch [:data model])
                                      (re-frame.core/dispatch [:data e])))))



