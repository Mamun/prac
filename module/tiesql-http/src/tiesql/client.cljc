(ns tiesql.client
  #?(:clj
     (:require [ajax.core :as a]
               [tiesql.util :as u]))
  #?@(:cljs
      [(:require [ajax.core :as a]
         [tiesql.common :as c]
         [tiesql.util :as u])]))


(defn build-ajax-request [params handler]
  {:method          :post
   :headers         {}
   ;:format          :transit
   :params          params
   :format          (a/transit-request-format)
   :response-format (a/transit-response-format)
   :handler         handler
   :error-handler   handler})


#_(def accept-html-options
    {:accept "text/html"
     :input  :string
     :output :string})


(def csrf-headers {"Accept" "application/transit+json"
                   ;"x-csrf-token" (.-value (.getElementById js/document "csrf-token"))
                   })


(defn pull
  [request-m handler & [url]]
  (->> handler
       (build-ajax-request (u/build-request request-m))
       (a/POST (str (or url "") "/pull"))))


(defn push!
  [request-m handler & [url]]
  (->> handler
       (build-ajax-request (u/build-request request-m))
       (a/POST (str (or url "") "/push"))))


#?(:cljs
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


(comment

  (let [v (pull
            :url "http://localhost:3001"
            :name :get-dept-by-id
            :params {:id 1}
            :callback (fn [w]
                        (print w)
                        ))]
    (println @v)
    )

  )