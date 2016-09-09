(ns model
  (:require [clojure.spec :as s]
            [clojure.walk :as w]))


(defn update-ns
  "Doc "
  [ns-str spec-list]
  (w/postwalk (fn [v]
                (if (and (keyword? v)
                         (= (namespace v) (str *ns*)))
                  (keyword (str ns-str "/" (name v)))
                  v)
                ) spec-list))


(defmacro in-spec
  ""
  [n & content]
  (let [content (update-ns n content)]
    `(do
       (clojure.core/in-ns '~(symbol n))
       (clojure.core/refer 'clojure.core)
       (clojure.core/require '[clojure.spec :as ~(symbol 's)])
       ~@content
       nil)))


(in-spec User
  (s/def ::id string?)
  (s/def ::spec (s/keys :req-un [::id])))


(in-spec Credit
  (s/def ::no number?)
  (s/def ::spec (s/keys :req-un [::id])))




(Model User {:id int?})


(s/valid? :User/spec {:id "Musterman"})
(s/valid? :Credit/spec {:id 3})


(s/valid? :User/id "Musterman")
(s/valid? :Credit/id 12345)
(s/valid? :Credit/id "Error")



(comment

  " I have different type of Model in my domain like User, Credit.
    I would like to define spec of the Model in one place but not different clj/cljs file.
    I don't want to create one clj file for one model. Because Clojure namespaces are very similar to Java packages.
    (https://github.com/clojuredocs/guides/blob/master/articles/language/namespaces.md)

    On the other hand, I could not defined same keyword with different predicate in one namespace.

    So what I did in bellow.


    Although I am creating namesapce here but it is not visable here.
    I would like to know Is there any better way to do or any core funtion that I could use here?




    "

  )








