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




