(ns app  
  (:require [clojure.spec]
            [dadyspec.core]))
(clojure.spec/def :app.student/id int?)
(clojure.spec/def :app/student (clojure.spec/keys :req-un [:app.student/id]))
(clojure.spec/def :app/student-list (clojure.spec/coll-of :app/student :kind clojure.core/vector?))
(clojure.spec/def :app-ex.student/id (clojure.spec/conformer dadyspec.core/x-int?))
(clojure.spec/def :app-ex/student (clojure.spec/keys :req-un [:app-ex.student/id]))
(clojure.spec/def :app-ex/student-list (clojure.spec/coll-of :app-ex/student :kind clojure.core/vector?))
