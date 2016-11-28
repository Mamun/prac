(defproject schufa-parser "0.1.0-alpha-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/tools.reader "0.9.2"]]
  :profiles {:dev     {:source-paths ["dev"]
                       :dependencies [[org.clojure/test.check "0.9.0"]
                                      [org.clojure/tools.nrepl "0.2.12"]]
                       :repl-options {:port             4555}}
             })

