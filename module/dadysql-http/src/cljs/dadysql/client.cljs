(ns dadysql.client
  (:require [ajax.core :as a]
            [dadysql.re-frame :as r]
            [dadysql.util :as u]))



(defn find-subscribe-key
  [input-request]
  (let [name (:dadysql.core/name input-request)
        group (:dadysql.core/group input-request)
        n (if (sequential? name)
            (first name)
            name)]
    (or group n)))



(defn build-request
  ([subscribe-key param-m]
   (let []
     {:params        param-m
      :handler       #(r/mutate-store subscribe-key %)
      :error-handler #(r/mutate-store subscribe-key %)}))
  ([param-m]
   (build-request (find-subscribe-key param-m) param-m)))


(def default-request {:method          :post
                      :headers         {}
                      :format          (a/transit-request-format)
                      :response-format (a/transit-response-format)
                      :handler         (fn [v] (js/console.log (str v)))
                      :error-handler   (fn [v] (js/console.log (str v)))})


(def csrf-headers {"Accept" "application/transit+json"
                   ;"x-csrf-token" (.-value (.getElementById js/document "csrf-token"))
                   })


(defn pull
  ([url ajax-m]
   (->> (merge default-request ajax-m)
        (a/POST (str (or url "") "/pull"))))
  ([ajax-m]
   (pull "" ajax-m)))


(defn push!
  ([url ajax-m]
   (->> (merge default-request ajax-m)
        (a/POST (str (or url "") "/push"))))
  ([ajax-m]
   (push! "" ajax-m)))



(comment

  (->> (default-dadysql-params {:a 3})
       (default-dadysql-params)
       (a/POST "/pull")
       )
  )


#_(:cljs
   (defn build-js-request
     [name params options callback]
     (let [namev (mapv keyword (js->clj name))
           params (js->clj params)
           options (js->clj options)
           cb (fn [res] (if (= "text/html" (get-in options [:accept]))
                          (callback res)
                          (callback (clj->js res))))]
       (merge {:name     namev
               :params   params
               :input    :string
               :output   :string
               :callback cb} options))))


#_(def ^:export h-options (clj->js accept-html-options))


