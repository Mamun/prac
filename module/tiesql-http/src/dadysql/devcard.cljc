(ns dadysql.devcard)


(defn devcard-callback []
  )


(defmacro defcard-dadysql
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
      (fn [~v]
                   (devcards.core/defcard ~name
                                          ~description
                                          ~v)))))






